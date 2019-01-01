import java.io.File
import java.io.FileNotFoundException
import java.io.FilenameFilter
import java.io.PrintWriter
import java.util.*
import java.util.regex.Pattern

fun main() {
    TikaSkeleton().run()
}

class TikaSkeleton {

    private var keywords: MutableList<String> = ArrayList()
    lateinit var logfile: PrintWriter
    private var numKeywords: Int = 0
    private var numFiles: Int = 0
    private var numFilesWithKeywords: Int = 0
    private var keywordCounts: MutableMap<String, Int>
    private var timestamp: Date

    /**
     * constructor
     * DO NOT MODIFY
     */
    init {
        numKeywords = 0
        numFiles = 0
        numFilesWithKeywords = 0
        keywordCounts = HashMap()
        timestamp = Date()
        try {
            logfile = PrintWriter("log.txt")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    /**
     * destructor
     * DO NOT MODIFY
     */
    @Throws(Throwable::class)
    private fun finalize() {
        try {
            logfile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * execute the program
     * DO NOT MODIFY
     */
    fun run() {

        // Open input file and read keywords
        File("./queries.txt").forEachLine { line ->
            keywords.add(line)
            numKeywords++
            keywordCounts[line] = 0
        }

        // Open all pdf files, process each one
        val pdfDir = File("./tikadataset")
        pdfDir.listFiles(PDFFilenameFilter()).forEach {
            numFiles++
            processfile(it)
        }

        // Print output file
        try {
            PrintWriter("output.txt").use {
                it.print("Keyword(s) used: ")
                if (numKeywords > 0) it.print(keywords[0])
                for (i in 1 until numKeywords) it.print(", " + keywords[i])
                it.println()
                it.println("No of files processed: $numFiles")
                it.println("No of files containing keyword(s): $numFilesWithKeywords")
                it.println()
                it.println("No of occurrences of each keyword:")
                it.println("----------------------------------")
                for (i in 0 until numKeywords) {
                    val keyword = keywords[i]
                    it.println("\t" + keyword + ": " + keywordCounts[keyword])
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    /**
     * Process a single file
     *
     * Here, you need to:
     * - use Tika to extract text contents from the file
     * - search the extracted text for the given keywords
     * - update numFilesWithKeywords and keywordCounts as needed
     * - update log file as needed
     *
     * @param f File to be processed
     */
    private fun processfile(f: File) {

        /***** YOUR CODE GOES HERE  */
        // to update the log file with information on the language, author, type, and last modification date implement
        // updatelogMetaData(f.getName());

        // to update the log file with a search hit, use:
        // 	updatelogHit(keyword,f.getName());

    }

    private fun updatelogMetaData(filename: String) {
        logfile.println(" --  data on file \"$filename\"")
        /***** YOUR CODE GOES HERE  */
        logfile.println()
        logfile.flush()
    }

    /**
     * Update the log file with search hit
     * Appends a log entry with the system timestamp, keyword found, and filename of PDF file containing the keyword
     * DO NOT MODIFY
     */
    private fun updatelogHit(keyword: String, filename: String) {
        timestamp.setTime(System.currentTimeMillis())
        logfile.println("$timestamp -- \"$keyword\" found in file \"$filename\"")
        logfile.flush()
    }

    /**
     * Filename filter that accepts only *.pdf
     * DO NOT MODIFY
     */
    internal class PDFFilenameFilter : FilenameFilter {
        private val p = Pattern.compile(".*\\.pdf", Pattern.CASE_INSENSITIVE)
        override fun accept(dir: File, name: String): Boolean {
            val m = p.matcher(name)
            return m.matches()
        }
    }


}