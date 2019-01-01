import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.SimpleCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.tika.Tika
import org.apache.tika.language.LanguageIdentifier
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


fun main() {
    Logger.getRootLogger().level = Level.OFF // Turn off warnings
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
    private val tika = Tika()
    private val indexPath = "index"
    private val analyzer = StandardAnalyzer()
    private val iPath = Paths.get(indexPath)
    private val directory = FSDirectory.open(iPath)
    private val indexWriter by lazy {
        val indexWriterConfig = IndexWriterConfig(analyzer)
        indexWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE
        IndexWriter(directory, indexWriterConfig)
    }

    private val indexSearcher: IndexSearcher by lazy {
        val path: Path? = Paths.get(indexPath)
        val directory: Directory = FSDirectory.open(path)
        val indexReader: IndexReader = DirectoryReader.open(directory)
        IndexSearcher(indexReader)
    }

    private val filenames = ArrayList<String>()

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
     * DO NOT MODIFY - needed to modify :C
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
            processFile(it)
        }

        // here
        indexWriter.close()
        searchKeywords()

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

    private fun searchKeywords() {
        // search in index
        val indexReader: IndexReader = DirectoryReader.open(directory)
        val indexSearcher = IndexSearcher(indexReader)

        val queryParser = QueryParser("fulltext", analyzer)
        for (keyword in keywords) {
            val query: Query = queryParser.parse(keyword)
            indexSearcher.search(query, object : SimpleCollector() {
                override fun needsScores() = false

                override fun collect(doc: Int) {
                    updateLogHit(keyword, filenames[doc])
                }
            })
        }
    }

    private fun indexDocument(filename: String, fullText: String) {
        // create index
        indexWriter.addDocument(Document().apply {
            add(
                TextField(
                    "filename", filename, Field.Store.YES
                )
            )
            add(
                TextField(
                    "fulltext", fullText, Field.Store.NO
                )
            )
        })

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
    private fun processFile(f: File) {


        // Get fulltext
        val fulltext = tika.parseToString(f)
        val filename = f.name
        filenames.add(filename)
        indexDocument(filename, fulltext)

        // parser
        val parser = AutoDetectParser()
        val metadata = Metadata()
        val inputStream = FileInputStream(f)
        val context = ParseContext()
        val handler = BodyContentHandler(-1) //No limit
        parser.parse(inputStream, handler, metadata, context)

        // Language
        val identifier = LanguageIdentifier(fulltext)

        updatelogMetaData(filename, metadata, identifier.language)

    }

    private fun updatelogMetaData(filename: String, metadata: Metadata, lang: String) {
        logfile.println(" --  data on file \"$filename\"")
        logfile.println("lang: $lang")
        logfile.println("author: ${metadata.get(Metadata.CREATOR)}")
        logfile.println("type: ${metadata.get(Metadata.CONTENT_TYPE)}")
        logfile.println("last modified: ${metadata.get(Metadata.LAST_MODIFIED)}")
        logfile.println()
        logfile.flush()
    }

    /**
     * Update the log file with search hit
     * Appends a log entry with the system timestamp, keyword found, and filename of PDF file containing the keyword
     * DO NOT MODIFY
     */
    private fun updateLogHit(keyword: String, filename: String) {
        timestamp.time = System.currentTimeMillis()
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