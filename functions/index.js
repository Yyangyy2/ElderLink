const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendSafeZoneAlert = functions.firestore
    .document("users/{caregiverUid}/people/{personUid}")
    .onUpdate(async (change, context) => {
      console.log("Safe Zone Alert Function Triggered");

      const after = change.after.data();
      const before = change.before.data();

      const {caregiverUid} = context.params;

      // Check if safeZoneAlert was added
      if (after.safeZoneAlert && !before.safeZoneAlert) {
        console.log("Safe zone alert detected, sending notification...");

        const alertData = after.safeZoneAlert;

        try {
          // Get caregiver's FCM token
          const caregiverDoc = await admin.firestore()
              .collection("users")
              .doc(caregiverUid)
              .get();

          if (!caregiverDoc.exists) {
            console.log("Caregiver document not found");
            return null;
          }

          const caregiverData = caregiverDoc.data();
          const fcmToken = caregiverData.fcmToken;

          if (!fcmToken) {
            console.log("No FCM token found for caregiver");
            return null;
          }

          console.log("Sending to FCM token:", fcmToken);

          // Create the notification message
          const message = {
            token: fcmToken,
            notification: {
              title: "Safe Zone Alert",
              body: `${alertData.personName} has left the safe zone! ` +
                  `Distance: ${alertData.distance}m`,
            },
            data: {
              type: "safe_zone_alert",
              personName: alertData.personName,
              distance: alertData.distance.toString(),
              timestamp: alertData.timestamp.toString(),
            },
            android: {
              priority: "high",
              notification: {
                sound: "default",
                channelId: "elderlink_safe_zone_alerts",
              },
            },
            apns: {
              payload: {
                aps: {
                  sound: "default",
                  badge: 1,
                },
              },
            },
          };

          // Send the notification
          const response = await admin.messaging().send(message);
          console.log("Notification sent successfully:", response);

          // Clear the alert flag to prevent duplicate notifications
          await change.after.ref.update({
            "safeZoneAlert": null,
          });

          console.log("Alert flag cleared");
        } catch (error) {
          console.error("Error sending notification:", error);
        }
      }

      return null;
    });

