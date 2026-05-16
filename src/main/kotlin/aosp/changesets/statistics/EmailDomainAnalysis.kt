package aosp.changesets.statistics

import com.fasterxml.jackson.core.type.TypeReference
import java.io.File

fun main() {
    val emailDomainToChangesetNum = mutableMapOf<String, Long>()
    val emailDomainToInsertionNum = mutableMapOf<String, Long>()
    val emailDomainToDeletionNum = mutableMapOf<String, Long>()
    val emailDomainToInsertionDeletionNum = mutableMapOf<String, Long>()

    val buildDir = File("build")
    val jsonFiles = buildDir.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
    if (jsonFiles.isEmpty()) {
        println("No JSON files found under ${buildDir.absolutePath}")
        return
    }

    jsonFiles.forEach {
        val changesets = objectMapper.readValue(it, object : TypeReference<List<Changeset>>() {})
        for (changeset in changesets) {
            val emailDomain = changeset.submitter?.email?.substringAfter("@") ?: changeset.owner?.email?.substringAfter("@") ?: continue

            val changesetNum = emailDomainToChangesetNum[emailDomain] ?: 0
            val insertion = emailDomainToInsertionNum[emailDomain] ?: 0
            val deletion = emailDomainToDeletionNum[emailDomain] ?: 0
            val insertionDeletion = emailDomainToInsertionDeletionNum[emailDomain] ?: 0

            val insertionOfCurrentChangeSet = if (changeset.insertions != null) changeset.insertions else 0
            val deletionOfCurrentChangeSet = if (changeset.deletions != null) changeset.deletions else 0

            emailDomainToChangesetNum[emailDomain] = changesetNum + 1
            emailDomainToInsertionNum[emailDomain] = insertion + insertionOfCurrentChangeSet
            emailDomainToDeletionNum[emailDomain] = deletion + deletionOfCurrentChangeSet
            emailDomainToInsertionDeletionNum[emailDomain] = insertionDeletion + deletionOfCurrentChangeSet + insertionOfCurrentChangeSet
        }
    }
    val analysisDir = File(buildDir, "analysis")
    analysisDir.mkdirs()

    println("domain by changeset number:")
    emailDomainToChangesetNum.toCsv(File(analysisDir, "domain_by_changeset_number.csv"))
    println("---------------")

    println("domain by insertion:")
    emailDomainToInsertionNum.toCsv(File(analysisDir, "domain_by_insertion.csv"))
    println("---------------")

    println("domain by deletion:")
    emailDomainToDeletionNum.toCsv(File(analysisDir, "domain_by_deletion.csv"))
    println("---------------")

    println("domain by insertion+deletion:")
    emailDomainToInsertionDeletionNum.toCsv(File(analysisDir, "domain_by_insertion_deletion.csv"))
    println("---------------")

    println("CSV outputs saved under ${analysisDir.absolutePath}")
}

fun MutableMap<String, Long>.toCsv(outputFile: File) {
    val lines = entries.sortedByDescending { it.value }
        .map { "${it.key.csvEscape()},${it.value}" }
    outputFile.printWriter().use { writer ->
        lines.forEach { writer.println(it) }
    }
    lines.forEach { println(it) }
}

fun String.csvEscape(): String {
    val needsQuote = contains(",") || contains("\"") || contains("\n") || contains("\r")
    val escaped = replace("\"", "\"\"")
    return if (needsQuote) {
        "\"$escaped\""
    } else {
        this
    }
}
