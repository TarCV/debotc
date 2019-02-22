import kotlinx.cinterop.*
import platform.posix.*
import com.github.tarcv.zandronum.debotc.Decompiler

fun main(args: Array<String>) {
    if (args.size != 1) {
        throw IllegalArgumentException("Only one argument - path to compiled script should be passed")
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

    val decompiler = Decompiler()
    decompiler.parse(data0)
    decompiler.print()
}