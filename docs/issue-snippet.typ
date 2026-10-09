#block(breakable: false)[
#raw("    @Transaction\n    suspend fun issueLoanAtomically(record: LoanRecord, maxActiveLoansPerReader: Int): Boolean {\n        if (record.status != \"ACTIVE\" || record.returnedAt != null) return false\n        if (countActiveLoans(record.readerId) >= maxActiveLoansPerReader) return false\n        if (decrementAvailable(record.publicationId) != 1) return false\n        insertLoan(record)\n        return true\n    }", lang: "kotlin", theme: none, block: true)
]
