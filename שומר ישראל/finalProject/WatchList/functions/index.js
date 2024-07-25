const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.getTeams = functions.https.onRequest(async (req, res) => {
    try {
        const teamsSnapshot = await db.collection("Teams").get();
        const teams = [];
        teamsSnapshot.forEach(doc => {
            teams.push(doc.id);
        });
        res.status(200).json(teams);
    } catch (error) {
        console.error("Error getting teams:", error);
        res.status(500).send("Error getting teams");
    }
});

exports.addTeam = functions.https.onRequest(async (req, res) => {
    const teamName = req.body.name;
    if (!teamName) {
        res.status(400).send('Team name is required');
        return;
    }
    try {
        await db.collection("Teams").doc(teamName).set({ name: teamName });
        res.status(200).send('Team added successfully');
    } catch (error) {
        console.error("Error adding team:", error);
        res.status(500).send('Error adding team');
    }
});
exports.deleteTeam = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;

    if (!teamName) {
        res.status(400).send("Team name is required");
        return;
    }

    try {
        await admin.firestore().collection("Teams").doc(teamName).delete();
        res.status(200).send("Team deleted successfully");
    } catch (error) {
        console.error("Error deleting team: ", error);
        res.status(500).send("Error deleting team: " + error.message);
    }
});
exports.getMembers = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    if (!teamName) {
        res.status(400).send('Team name is required');
        return;
    }
    try {
        const teamDoc = await db.collection("Teams").doc(teamName).get();
        if (teamDoc.exists) {
            res.status(200).json(teamDoc.data().members || {});
        } else {
            res.status(404).send('Team not found');
        }
    } catch (error) {
        console.error("Error getting members:", error);
        res.status(500).send('Error getting members');
    }
});

exports.updateMembers = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    const members = req.body;
    if (!teamName || !members) {
        res.status(400).send('Team name and members are required');
        return;
    }
    try {
        await db.collection("Teams").doc(teamName).update({ members });
        res.status(200).send('Members updated successfully');
    } catch (error) {
        console.error("Error updating members:", error);
        res.status(500).send('Error updating members');
    }
});


exports.deleteMember = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    const memberName = req.query.memberName;
    if (!teamName || !memberName) {
        res.status(400).send('Team name and member name are required');
        return;
    }
    try {
        const teamDoc = await db.collection("Teams").doc(teamName).get();
        if (teamDoc.exists) {
            const members = teamDoc.data().members;
            if (members && members.hasOwnProperty(memberName)) {
                delete members[memberName];
                await db.collection("Teams").doc(teamName).update({ members });
                res.status(200).send('Member deleted successfully');
            } else {
                res.status(404).send('Member not found');
            }
        } else {
            res.status(404).send('Team not found');
        }
    } catch (error) {
        console.error("Error deleting member:", error);
        res.status(500).send('Error deleting member');
    }
});


exports.changeTeamName = functions.https.onRequest(async (req, res) => {
    const oldTeamName = req.query.oldTeamName;
    const newTeamName = req.query.newTeamName;
    if (!oldTeamName || !newTeamName) {
        res.status(400).send('Old team name and new team name are required');
        return;
    }
    try {
        const oldDocRef = db.collection("Teams").doc(oldTeamName);
        const newDocRef = db.collection("Teams").doc(newTeamName);
        const oldDoc = await oldDocRef.get();
        if (oldDoc.exists) {
            const teamData = oldDoc.data();
            teamData.name = newTeamName;
            await newDocRef.set(teamData);
            await oldDocRef.delete();
            res.status(200).send('Team name changed successfully');
        } else {
            res.status(404).send('Old team not found');
        }
    } catch (error) {
        console.error("Error changing team name:", error);
        res.status(500).send('Error changing team name');
    }
});

exports.getWatchLists = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    if (!teamName) {
        res.status(400).send('Team name is required');
        return;
    }
    try {
        const listsSnapshot = await db.collection("Teams").doc(teamName).collection("Lists").get();
        const watchLists = [];
        listsSnapshot.forEach(doc => {
            const data = doc.data();
            data.listName = doc.id;
            watchLists.push(data);
        });
        res.status(200).json(watchLists);
    } catch (error) {
        console.error("Error getting watch lists:", error);
        res.status(500).send("Error getting watch lists");
    }
});

exports.createWatchList = functions.https.onRequest(async (req, res) => {
    const { teamName, listName, timestamp } = req.body;
    if (!teamName || !listName || !timestamp) {
        res.status(400).send('Team name, list name, and timestamp are required');
        return;
    }
    try {
        await db.collection("Teams").doc(teamName).collection("Lists").doc(listName).set({ timestamp });
        res.status(200).send('Watch list created successfully');
    } catch (error) {
        console.error("Error creating watch list:", error);
        res.status(500).send('Error creating watch list');
    }
});

exports.deleteWatchList = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    const listName = req.query.listName;
    if (!teamName || !listName) {
        res.status(400).send('Team name and list name are required');
        return;
    }
    try {
        await db.collection("Teams").doc(teamName).collection("Lists").doc(listName).delete();
        res.status(200).send('Watch list deleted successfully');
    } catch (error) {
        console.error("Error deleting watch list:", error);
        res.status(500).send('Error deleting watch list');
    }
});

exports.getWatchList = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    const listName = req.query.listName;

    if (!teamName || !listName) {
        res.status(400).send("Team name and list name are required");
        return;
    }

    try {
        const doc = await admin.firestore().collection("Teams").doc(teamName).collection("Lists").doc(listName).get();
        if (doc.exists) {
            res.status(200).json(doc.data());
        } else {
            res.status(404).send("Document not found");
        }
    } catch (error) {
        console.error("Error fetching document: ", error);
        res.status(500).send("Error fetching document: " + error.message);
    }
});

exports.saveSchedule = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    const listName = req.query.listName;
    const scheduleData = req.body;

    if (!teamName || !listName || !scheduleData) {
        res.status(400).send("Team name, list name, and schedule data are required");
        return;
    }

    try {
        await admin.firestore().collection("Teams").doc(teamName).collection("Lists").doc(listName).update(scheduleData);
        res.status(200).send("Schedule successfully saved!");
    } catch (error) {
        console.error("Error saving schedule: ", error);
        res.status(500).send("Error saving schedule: " + error.message);
    }
});

exports.addList = functions.https.onRequest(async (req, res) => {
    try {
        const { teamName } = req.query;
        const listData = req.body.listData;
        const listName = listData.listName;

        await db.collection('Teams').doc(teamName).collection('Lists').doc(listName).set(listData);
        res.status(200).send();
    } catch (error) {
        console.error('Error adding list:', error);
        res.status(500).send('Error adding list');
    }
});

exports.deleteList = functions.https.onRequest(async (req, res) => {
    const teamName = req.query.teamName;
    const listName = req.query.listName;

    if (!teamName || !listName) {
        res.status(400).send("Team name and list name are required");
        return;
    }

    try {
        await admin.firestore().collection("Teams").doc(teamName).collection("Lists").doc(listName).delete();
        res.status(200).send("Document successfully deleted!");
    } catch (error) {
        console.error("Error deleting document: ", error);
        res.status(500).send("Error deleting document: " + error.message);
    }
});
