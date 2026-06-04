package com.barisdincer.circlekeep

import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PersonContactRhythm
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.presentation.InteractionLogQuery
import com.barisdincer.circlekeep.data.presentation.InteractionLogView
import com.barisdincer.circlekeep.data.presentation.PeopleListQuery
import com.barisdincer.circlekeep.data.presentation.PeopleListView
import com.barisdincer.circlekeep.data.presentation.buildDashboardPresentation
import com.barisdincer.circlekeep.data.presentation.buildInteractionLogPresentation
import com.barisdincer.circlekeep.data.presentation.buildPeopleListItems
import com.barisdincer.circlekeep.data.presentation.buildReportsSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

class PresentationDomainTest {
    @Test
    fun `people query filters waiting upcoming and untagged views`() {
        val now = 100L * DAY_MILLIS
        val close = Wave(id = 1, name = "Yakınlar", frequencyDays = 7)
        val people = listOf(
            Person(id = 1, name = "Ayse", phoneNumber = "1", waveId = close.id, tags = "okul", lastInteractionDate = now - 10L * DAY_MILLIS),
            Person(id = 2, name = "Mehmet", phoneNumber = "2", waveId = close.id, tags = "", lastInteractionDate = now - 4L * DAY_MILLIS),
            Person(id = 3, name = "Zeynep", phoneNumber = "3", waveId = close.id, tags = "aile", lastInteractionDate = now - 1L * DAY_MILLIS)
        )

        val waiting = buildPeopleListItems(
            people = people,
            waves = listOf(close),
            rhythms = emptyList(),
            query = PeopleListQuery(view = PeopleListView.OVERDUE),
            currentTimeMillis = now
        )
        val upcoming = buildPeopleListItems(
            people = people,
            waves = listOf(close),
            rhythms = emptyList(),
            query = PeopleListQuery(view = PeopleListView.UPCOMING),
            currentTimeMillis = now
        )
        val untagged = buildPeopleListItems(
            people = people,
            waves = listOf(close),
            rhythms = emptyList(),
            query = PeopleListQuery(view = PeopleListView.UNTAGGED),
            currentTimeMillis = now
        )

        assertEquals(listOf("Ayse"), waiting.map { it.person.name })
        assertEquals(listOf("Mehmet", "Zeynep"), upcoming.map { it.person.name })
        assertEquals(listOf("Mehmet"), untagged.map { it.person.name })
    }

    @Test
    fun `interaction log presentation groups events and searches participants`() {
        val now = 100L * DAY_MILLIS
        val wave = Wave(id = 1, name = "Arkadaşlar", frequencyDays = 21)
        val people = listOf(
            Person(id = 1, name = "Ayse", phoneNumber = "1", waveId = wave.id),
            Person(id = 2, name = "Mehmet", phoneNumber = "2", waveId = wave.id)
        )
        val logs = listOf(
            InteractionLog(id = 1, personId = 1, timestamp = now, type = DefaultContactTypes.MEETING, note = "Kahve"),
            InteractionLog(id = 2, personId = 2, timestamp = now + 1_000L, type = DefaultContactTypes.MEETING, note = "Kahve"),
            InteractionLog(id = 3, personId = 2, timestamp = now - 40L * DAY_MILLIS, type = DefaultContactTypes.CALL, note = "Kısa")
        )

        val all = buildInteractionLogPresentation(
            logs = logs,
            people = people,
            waves = listOf(wave),
            contactTypes = emptyList(),
            query = InteractionLogQuery(searchTerm = "Ayse"),
            currentTimeMillis = now
        )
        val recent = buildInteractionLogPresentation(
            logs = logs,
            people = people,
            waves = listOf(wave),
            contactTypes = emptyList(),
            query = InteractionLogQuery(view = InteractionLogView.LAST_30_DAYS),
            currentTimeMillis = now
        )

        assertEquals(1, all.eventCount)
        assertEquals(listOf("Ayse"), all.events.single().participantNames)
        assertEquals(2, recent.filteredRecordCount)
        assertEquals(1, recent.eventCount)
    }

    @Test
    fun `dashboard presentation exposes calm and waiting counts`() {
        val now = 100L * DAY_MILLIS
        val wave = Wave(id = 1, name = "Yakınlar", frequencyDays = 7)
        val people = listOf(
            Person(id = 1, name = "Ayse", phoneNumber = "1", waveId = wave.id, lastInteractionDate = now - 9L * DAY_MILLIS),
            Person(id = 2, name = "Mehmet", phoneNumber = "2", waveId = wave.id, lastInteractionDate = now - 1L * DAY_MILLIS)
        )

        val presentation = buildDashboardPresentation(
            people = people,
            waves = listOf(wave),
            rhythms = emptyList(),
            contactTypes = emptyList(),
            interactions = emptyList(),
            currentTimeMillis = now
        )

        assertEquals(1, presentation.overdueCount)
        assertEquals(0, presentation.todayCount)
        assertEquals(1, presentation.upcomingCount)
        assertEquals("Ayse", presentation.focusContacts.single().personName)
    }

    @Test
    fun `reports summary keeps chart math outside compose`() {
        val now = 100L * DAY_MILLIS
        val wave = Wave(id = 1, name = "Yakınlar", frequencyDays = 7)
        val people = listOf(
            Person(id = 1, name = "Ayse", phoneNumber = "1", waveId = wave.id, lastInteractionDate = now - 8L * DAY_MILLIS),
            Person(id = 2, name = "Mehmet", phoneNumber = "2", waveId = null, lastInteractionDate = now - 70L * DAY_MILLIS)
        )
        val rhythms = listOf(PersonContactRhythm(personId = 1, contactTypeKey = DefaultContactTypes.CALL))
        val logs = listOf(
            InteractionLog(id = 1, personId = 1, timestamp = now, type = DefaultContactTypes.CALL),
            InteractionLog(id = 2, personId = 2, timestamp = now - 40L * DAY_MILLIS, type = DefaultContactTypes.MESSAGE)
        )

        val summary = buildReportsSummary(
            people = people,
            waves = listOf(wave),
            rhythms = rhythms,
            contactTypes = emptyList(),
            interactions = logs,
            currentTimeMillis = now
        )

        assertEquals(2, summary.peopleCount)
        assertEquals(1, summary.recentInteractionCount)
        assertEquals(1, summary.reachedPeopleCount)
        assertTrue(summary.rhythmRows.any { it.key == "overdue" && it.count == 1 })
        assertTrue(summary.groupRows.any { it.label == "Grup yok" && it.count == 1 })
    }
}
