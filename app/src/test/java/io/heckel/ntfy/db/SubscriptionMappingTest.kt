package io.heckel.ntfy.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionMappingTest {

    @Test
    fun `should include latest notification title and message when available`() {
        val withMeta = SubscriptionWithMetadata(
            id = 1,
            baseUrl = "https://ntfy.sh",
            topic = "test",
            instant = false,
            mutedUntil = 0,
            autoDelete = -1,
            minPriority = 0,
            insistent = -1,
            lastNotificationId = "abc",
            icon = null,
            upAppId = null,
            upConnectorToken = null,
            displayName = null,
            dedicatedChannels = false,
            totalCount = 5,
            newCount = 2,
            lastActive = 1717920000,
            latestTitle = "Backup Completed",
            latestMessage = "All backups finished successfully"
        )

        val subscription = Repository.mapSubscriptionWithMetadata(withMeta)

        assertEquals("Backup Completed", subscription.latestTitle)
        assertEquals("All backups finished successfully", subscription.latestMessage)
    }

    @Test
    fun `should set latest title and message to null when empty strings`() {
        val withMeta = SubscriptionWithMetadata(
            id = 2,
            baseUrl = "https://ntfy.sh",
            topic = "empty",
            instant = false,
            mutedUntil = 0,
            autoDelete = -1,
            minPriority = 0,
            insistent = -1,
            lastNotificationId = null,
            icon = null,
            upAppId = null,
            upConnectorToken = null,
            displayName = null,
            dedicatedChannels = false,
            totalCount = 0,
            newCount = 0,
            lastActive = 0,
            latestTitle = "",
            latestMessage = ""
        )

        val subscription = Repository.mapSubscriptionWithMetadata(withMeta)

        assertNull(subscription.latestTitle)
        assertNull(subscription.latestMessage)
    }

    @Test
    fun `should set latest title and message to null when not available`() {
        val withMeta = SubscriptionWithMetadata(
            id = 3,
            baseUrl = "https://ntfy.sh",
            topic = "new",
            instant = false,
            mutedUntil = 0,
            autoDelete = -1,
            minPriority = 0,
            insistent = -1,
            lastNotificationId = null,
            icon = null,
            upAppId = null,
            upConnectorToken = null,
            displayName = null,
            dedicatedChannels = false,
            totalCount = 0,
            newCount = 0,
            lastActive = 0,
            latestTitle = null,
            latestMessage = null
        )

        val subscription = Repository.mapSubscriptionWithMetadata(withMeta)

        assertNull(subscription.latestTitle)
        assertNull(subscription.latestMessage)
    }
}
