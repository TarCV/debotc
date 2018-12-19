import com.github.tarcv.zandronum.debotc.BotCommand
import com.github.tarcv.zandronum.debotc.DataHeaders
import org.junit.Assert.assertEquals
import org.junit.Test

class ConstantTest {
    @Test
    fun dhConstants() {
        assertEquals(0, DataHeaders.DH_COMMAND.ordinal)
        assertEquals(10, DataHeaders.DH_ENDEVENT.ordinal)
        assertEquals(20, DataHeaders.DH_NOTEQUALS.ordinal)
        assertEquals(30, DataHeaders.DH_UNARYMINUS.ordinal)
        assertEquals(40, DataHeaders.DH_STRINGLIST.ordinal)
        assertEquals(50, DataHeaders.DH_DECLOCALVAR.ordinal)
        assertEquals(60, DataHeaders.DH_DECGLOBALARRAY.ordinal)
        assertEquals(70, DataHeaders.DH_ARRAYSET.ordinal)
    }

    @Test
    fun botCmdConstants() {
        assertEquals(0, BotCommand.BOTCMD_CHANGESTATE.ordinal)
        assertEquals(10, BotCommand.BOTCMD_LOOKFORSUPERARMOR.ordinal)
        assertEquals(20, BotCommand.BOTCMD_CHECKTERRAIN.ordinal)
        assertEquals(30, BotCommand.BOTCMD_BEGINAIMINGATENEMY.ordinal)
        assertEquals(40, BotCommand.BOTCMD_GETENEMYINVULNERABILITYTICKS.ordinal)
        assertEquals(50, BotCommand.BOTCMD_SAYFROMFILE.ordinal)
        assertEquals(60, BotCommand.BOTCMD_BEGINJUMPING.ordinal)
        assertEquals(66, BotCommand.BOTCMD_ISSPECTATING.ordinal)
        assertEquals(70, BotCommand.BOTCMD_GETBASEARMOR.ordinal)
        assertEquals(80, BotCommand.BOTCMD_SETSKILLDECREASE.ordinal)
        assertEquals(90, BotCommand.BOTCMD_SAYFROMCHATLUMP.ordinal)
        assertEquals(92, BotCommand.BOTCMD_CHATSECTIONEXISTSINCHATLUMP.ordinal)
    }
}