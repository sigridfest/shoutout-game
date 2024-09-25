const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.deleteOldDocuments = functions
    .pubsub
    .schedule("every 24 hours")
    .onRun(async (context) => {
      const firestore = admin.firestore();
      const now = admin.firestore.Timestamp.now();
      const oneDayAgo = new admin
          .firestore
          .Timestamp(now.seconds - 86400, now.nanoseconds);

      const query = firestore
          .collection("games")
          .where("created", "<", oneDayAgo);

      const snapshot = await query.get();
      const deletePromises = [];

      snapshot.forEach((doc) => {
        deletePromises.push(doc.ref.delete());
      });

      await Promise.all(deletePromises);
    });


exports.processAnswers = functions.firestore
    .document("games/{gameId}/questions/{questionId}/answers/{answerId}")
    .onCreate(async (snap, context) => {
      const gameId = context.params.gameId;
      const questionId = context.params.questionId;

      const gameRef = admin.firestore().collection("games").doc(gameId);
      const questionRef = gameRef.collection("questions").doc(questionId);

      const playersSnap = await gameRef.collection("players").get();
      const numPlayers = playersSnap.size;

      // Use a transaction to atomically increment the answer count
      const shouldProcess = await admin
          .firestore()
          .runTransaction(async (transaction) => {
            const questionDoc = await transaction.get(questionRef);
            const answerCount = questionDoc.get("answerCount") || 0;
            const endTime = questionDoc.get("endTime");

            if (answerCount + 1 === numPlayers && !endTime) {
              // Increment the answer count and mark the question as completed
              transaction.update(questionRef, {
                answerCount: admin.firestore.FieldValue.increment(1),
                endTime: admin.firestore.FieldValue.serverTimestamp(),
              });

              return true;
            } else {
              // Increment the answer count
              transaction.update(questionRef, {
                answerCount: admin.firestore.FieldValue.increment(1),
              });

              return false;
            }
          });

      if (shouldProcess) {
        await calculateResults(gameId, questionId);
      }
    });

exports.setTimerOnQuestion = functions.firestore
    .document("games/{gameId}/questions/{questionId}")
    .onUpdate(async (change, context) => {
      const questionData = change.after.data();
      const startTime = questionData.startTime;
      const previousStartTime = change.before.data().startTime;

      if (startTime && !previousStartTime) {
        const gameId = context.params.gameId;
        const questionId = context.params.questionId;
        setTimeout(() => {
          timeOut(gameId, questionId);
        }, 20000);
      }
    });

/**
 *
 * @param {*} gameId - The ID of the game document.
 * @param {*} questionId - The ID of the question document.
 */
async function timeOut(gameId, questionId) {
  const gameRef = admin.firestore().collection("games").doc(gameId);
  const questionRef = gameRef.collection("questions").doc(questionId);

  const shouldProcess = await admin
      .firestore()
      .runTransaction(async (transaction) => {
        const questionSnap = await questionRef.get();
        const endTime = questionSnap.get("endTime");
        if (!endTime) {
          transaction.update(questionRef, {
            endTime: admin.firestore.FieldValue.serverTimestamp(),
          });
          return true;
        } else {
          return false;
        }
      });

  if (shouldProcess) {
    await calculateResults(gameId, questionId);
  }
}


/**
 *
 * @param {string} gameId - The ID of the game document.
 * @param {string} questionId - The ID of the question document.
 */
async function calculateResults(gameId, questionId) {
  const gameRef = admin.firestore().collection("games").doc(gameId);
  const questionRef = gameRef.collection("questions").doc(questionId);
  const questionSnapshot = await questionRef.get();
  const scoreMultiplier = questionSnapshot.get("scoreMultiplier");
  const playersSnap = await gameRef.collection("players").get();

  questionRef.collection("answers").get().then((answersSnap) => {
    const answers = answersSnap.docs.map((doc) => doc.data());

    let superShoutOut = true;

    const results = new Map();

    // The result map shows how many players have given each answer
    for (const player of playersSnap.docs) {
      results.set(player.id, 0);
    }

    if (answers.length !== 0) {
      const firstAnswer = answers[0].answer;
      for (const answer of answers) {
        if (answer.answer !== firstAnswer) {
          superShoutOut = false;
        }
        const player = answer.answer;
        results.set(player, results.get(player) + 1);
      }
    } else {
      superShoutOut = false;
    }

    if (superShoutOut) {
      setSuperShoutOut(gameId, questionId);
    }

    for (const answer of answers) {
      const playerId = answer.playerId;
      const sameAnswers = results.get(answer.answer);
      const points = sameAnswers === 1 ? 0 :
          sameAnswers * 100 * scoreMultiplier;
      const playerRef = gameRef.collection("players").doc(playerId);
      playerRef.update({
        score: admin.firestore.FieldValue.increment(points),
      });
    }
  });
}

/**
 *
 * @param {*} gameId - The ID of the game document.
 * @param {*} questionId - The ID of the previous question document.
 */
async function setSuperShoutOut(gameId, questionId) {
  const gameRef = admin.firestore().collection("games").doc(gameId);
  const questionsSnap = await gameRef
      .collection("questions")
      .orderBy("index")
      .get();
  let superShoutOutReached = false;

  for (const question of questionsSnap.docs) {
    if (superShoutOutReached) {
      return question.ref.update({
        scoreMultiplier: 2,
      });
    }
    if (question.id === questionId) {
      superShoutOutReached = true;
    }
  }
}
