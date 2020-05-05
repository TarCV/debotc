import kotlinx.cinterop.*
import platform.posix.*
import com.github.tarcv.zandronum.debotc.Decompiler
import com.github.tarcv.zandronum.debotc.Printer
import com.github.tarcv.zandronum.debotc.FilePrinter
import com.github.tarcv.zandronum.debotc.consolePrinter
import com.github.tarcv.zandronum.debotc.lineSeparator

fun main(args: Array<String>) {
    if (args.size != 1 && args.size != 2) {
        throw IllegalArgumentException("Wrong arguments${lineSeparator}debotc <script.o> [<script.botc>]")
    }
    var data0 = UByteArray(0)

    val file = fopen(args[0], "rb")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt();
        rewind(file)
        memScoped {
            val buffer = allocArray<ByteVar>(size+1)
            val elementsRead = fread(buffer, size.convert<size_t>(), 1, file).toInt()
            if (elementsRead != 1) {
                throw IllegalStateException("Failed to read file")
            }
            data0 = buffer.readBytes(size).toUByteArray()
        }
    } finally {
        fclose(file)
    }

    val printer: Printer = if (args.size == 2) {
        FilePrinter(args[1])
    } else {
        consolePrinter
    }

    try {
        val decompiler = Decompiler()
        decompiler.parse(data0)
        decompiler.print(printer)
    } finally {
        printer.close()
    }
}