package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.BotCommandReturnType.*
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.*
import com.github.tarcv.zandronum.debotc.StackChangingNode.ArgumentHolder
import com.github.tarcv.zandronum.debotc.StackChangingNode.ArgumentHolder.Companion.createStringStackArgument
import com.github.tarcv.zandronum.debotc.StackChangingNode.Companion.consumesNormalStack

enum class BotCommand(
        val readableName: String,
        val numArgs: Int,
        val numStringArgs: Int,
        val returnType: BotCommandReturnType
) {
    BOTCMD_CHANGESTATE("changestate", 1, 0, RETURNVAL_VOID),
    BOTCMD_DELAY("delay", 1, 0, RETURNVAL_VOID),
    BOTCMD_RAND("Random", 2, 0, RETURNVAL_INT),
    BOTCMD_STRINGSAREEQUAL("StringsAreEqual", 0, 2, RETURNVAL_BOOLEAN),
    BOTCMD_LOOKFORPOWERUPS("LookForPowerups", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORWEAPONS("LookForWeapons", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORAMMO("LookForAmmo", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORBASEHEALTH("LookForBaseHealth", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORBASEARMOR("LookForBaseArmor", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORSUPERHEALTH("LookForSuperHealth", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORSUPERARMOR("LookForSuperArmor", 2, 0, RETURNVAL_INT),
    BOTCMD_LOOKFORPLAYERENEMIES("LookForPlayerEnemies", 1, 0, RETURNVAL_INT),
    BOTCMD_GETCLOSESTPLAYERENEMY("GetClosestPlayerEnemy", 0, 0, RETURNVAL_INT),
    BOTCMD_MOVELEFT("MoveLeft", 1, 0, RETURNVAL_VOID),
    BOTCMD_MOVERIGHT("MoveRight", 1, 0, RETURNVAL_VOID),
    BOTCMD_MOVEFORWARD("MoveForward", 1, 0, RETURNVAL_VOID),
    BOTCMD_MOVEBACKWARDS("MoveBackwards", 1, 0, RETURNVAL_VOID),
    BOTCMD_STOPMOVEMENT("StopMovement", 0, 0, RETURNVAL_VOID),
    BOTCMD_STOPFORWARDMOVEMENT("StopForwardMovement", 0, 0, RETURNVAL_VOID),
    BOTCMD_STOPSIDEWAYSMOVEMENT("StopSidewaysMovement", 0, 0, RETURNVAL_VOID),
    BOTCMD_CHECKTERRAIN("CheckTerrain", 2, 0, RETURNVAL_INT),
    BOTCMD_PATHTOGOAL("PathToGoal", 1, 0, RETURNVAL_INT),
    BOTCMD_PATHTOLASTKNOWNENEMYPOSITION("PathToLastKnownEnemyPosition", 1, 0, RETURNVAL_INT),
    BOTCMD_PATHTOLASTHEARDSOUND("PathToLastHeardSound", 1, 0, RETURNVAL_INT),
    BOTCMD_ROAM("Roam", 1, 0, RETURNVAL_INT),
    BOTCMD_GETPATHINGCOSTTOITEM("GetPathingCostToItem", 1, 0, RETURNVAL_INT),
    BOTCMD_GETDISTANCETOITEM("GetDistanceToItem", 1, 0, RETURNVAL_INT),
    BOTCMD_GETITEMNAME("GetItemName", 1, 0, RETURNVAL_STRING),
    BOTCMD_ISITEMVISIBLE("IsItemVisible", 1, 0, RETURNVAL_BOOLEAN),
    BOTCMD_SETGOAL("SetGoal", 1, 0, RETURNVAL_VOID),
    BOTCMD_BEGINAIMINGATENEMY("BeginAimingAtEnemy", 0, 0, RETURNVAL_VOID),
    BOTCMD_STOPAIMINGATENEMY("StopAimingAtEnemy", 0, 0, RETURNVAL_VOID),
    BOTCMD_TURN("Turn", 1, 0, RETURNVAL_VOID),
    BOTCMD_GETCURRENTANGLE("GetCurrentAngle", 0, 0, RETURNVAL_INT),
    BOTCMD_SETENEMY("SetEnemy", 1, 0, RETURNVAL_VOID),
    BOTCMD_CLEARENEMY("ClearEnemy", 0, 0, RETURNVAL_VOID),
    BOTCMD_ISENEMYALIVE("IsEnemyAlive", 0, 0, RETURNVAL_BOOLEAN),
    BOTCMD_ISENEMYVISIBLE("IsEnemyVisible", 0, 0, RETURNVAL_BOOLEAN),
    BOTCMD_GETDISTANCETOENEMY("GetDistanceToEnemy", 0, 0, RETURNVAL_INT),
    BOTCMD_GETPLAYERDAMAGEDBY("GetPlayerDamagedBy", 0, 0, RETURNVAL_INT),
    BOTCMD_GETENEMYINVULNERABILITYTICKS("GetEnemyInvulnerabilityTicks", 0, 0, RETURNVAL_INT),
    BOTCMD_FIREWEAPON("FireWeapon", 0, 0, RETURNVAL_VOID),
    BOTCMD_BEGINFIRINGWEAPON("BeginFiringWeapon", 0, 0, RETURNVAL_VOID),
    BOTCMD_STOPFIRINGWEAPON("StopFiringWeapon", 0, 0, RETURNVAL_VOID),
    BOTCMD_GETCURRENTWEAPON("GetCurrentWeapon", 0, 0, RETURNVAL_STRING),
    BOTCMD_CHANGEWEAPON("ChangeWeapon", 0, 1, RETURNVAL_VOID),
    BOTCMD_GETWEAPONFROMITEM("GetWeaponFromItem", 1, 0, RETURNVAL_STRING),
    BOTCMD_ISWEAPONOWNED("IsWeaponOwned", 1, 0, RETURNVAL_BOOLEAN),
    BOTCMD_ISFAVORITEWEAPON("IsFavoriteWeapon", 0, 1, RETURNVAL_BOOLEAN),
    BOTCMD_SAY("Say", 0, 1, RETURNVAL_VOID),
    BOTCMD_SAYFROMFILE("SayFromFile", 0, 2, RETURNVAL_VOID),
    BOTCMD_SAYFROMCHATFILE("SayFromChatFile", 0, 1, RETURNVAL_VOID),
    BOTCMD_BEGINCHATTING("BeginChatting", 0, 0, RETURNVAL_VOID),
    BOTCMD_STOPCHATTING("StopChatting", 0, 0, RETURNVAL_VOID),
    BOTCMD_CHATSECTIONEXISTS("ChatSectionExists", 0, 1, RETURNVAL_BOOLEAN),
    BOTCMD_CHATSECTIONEXISTSINFILE("ChatSectionExistsInFile", 0, 2, RETURNVAL_BOOLEAN),
    BOTCMD_GETLASTCHATSTRING("GetLastChatString", 0, 0, RETURNVAL_STRING),
    BOTCMD_GETLASTCHATPLAYER("GetLastChatPlayer", 0, 0, RETURNVAL_STRING),
    BOTCMD_GETCHATFREQUENCY("GetChatFrequency", 0, 0, RETURNVAL_INT),
    BOTCMD_JUMP("Jump", 0, 0, RETURNVAL_VOID),
    BOTCMD_BEGINJUMPING("BeginJumping", 0, 0, RETURNVAL_VOID),
    BOTCMD_STOPJUMPING("StopJumping", 0, 0, RETURNVAL_VOID),
    BOTCMD_TAUNT("Taunt", 0, 0, RETURNVAL_VOID),
    BOTCMD_RESPAWN("Respawn", 0, 0, RETURNVAL_VOID),
    BOTCMD_TRYTOJOINGAME("TryToJoinGame", 0, 0, RETURNVAL_VOID),
    BOTCMD_ISDEAD("IsDead", 0, 0, RETURNVAL_BOOLEAN),
    BOTCMD_ISSPECTATING("IsSpectating", 0, 0, RETURNVAL_BOOLEAN),
    BOTCMD_GETHEALTH("GetHealth", 0, 0, RETURNVAL_INT),
    BOTCMD_GETARMOR("GetArmor", 0, 0, RETURNVAL_INT),
    BOTCMD_GETBASEHEALTH("GetBaseHealth", 0, 0, RETURNVAL_INT),
    BOTCMD_GETBASEARMOR("GetBaseArmor", 0, 0, RETURNVAL_INT),
    BOTCMD_GETBOTSKILL("GetBotskill", 0, 0, RETURNVAL_INT),
    BOTCMD_GETACCURACY("GetAccuracy", 0, 0, RETURNVAL_INT),
    BOTCMD_GETINTELLECT("GetIntellect", 0, 0, RETURNVAL_INT),
    BOTCMD_GETANTICIPATION("GetAnticipation", 0, 0, RETURNVAL_INT),
    BOTCMD_GETEVADE("GetEvade", 0, 0, RETURNVAL_INT),
    BOTCMD_GETREACTIONTIME("GetReactionTime", 0, 0, RETURNVAL_INT),
    BOTCMD_GETPERCEPTION("GetPerception", 0, 0, RETURNVAL_INT),
    BOTCMD_SETSKILLINCREASE("SetSkillIncrease", 1, 0, RETURNVAL_VOID),
    BOTCMD_ISSKILLINCREASED("IsSkillIncreased", 0, 0, RETURNVAL_BOOLEAN),
    BOTCMD_SETSKILLDECREASE("SetSkillDecrease", 1, 0, RETURNVAL_VOID),
    BOTCMD_ISSKILLDECREASED("IsSkillDecreased", 0, 0, RETURNVAL_BOOLEAN),
    BOTCMD_GETGAMEMODE("GetGameMode", 0, 0, RETURNVAL_INT),
    BOTCMD_GETSPREAD("GetSpread", 0, 0, RETURNVAL_INT),
    BOTCMD_GETLASTJOINEDPLAYER("GetLastJoinedPlayer", 0, 0, RETURNVAL_STRING),
    BOTCMD_GETPLAYERNAME("GetPlayerName", 1, 0, RETURNVAL_STRING),
    BOTCMD_GETRECEIVEDMEDAL("GetReceivedMedal", 0, 0, RETURNVAL_INT),
    BOTCMD_ACS_EXECUTE("ACS_Execute", 5, 0, RETURNVAL_VOID),
    BOTCMD_GETFAVORITEWEAPON("GetFavoriteWeapon", 0, 0, RETURNVAL_STRING),
    BOTCMD_SAYFROMLUMP("SayFromLump", 0, 2, RETURNVAL_VOID),
    BOTCMD_SAYFROMCHATLUMP("SayFromChatLump", 0, 1, RETURNVAL_VOID),
    BOTCMD_CHATSECTIONEXISTSINLUMP("ChatSectionExistsInLump", 0, 2, RETURNVAL_BOOLEAN),
    BOTCMD_CHATSECTIONEXISTSINCHATLUMP("ChatSectionExistsInChatLump", 0, 1, RETURNVAL_BOOLEAN),

    NUM_BOTCMDS("", 0, 0, RETURNVAL_VOID);

    open fun createNode(): FunctionNode {
        val args = ArrayList<ArgumentHolder>()
        args.addAll(consumesNormalStack(numArgs))

        val stringArgs = StringBuilder()
        for (i in numStringArgs - 1 downTo 0) {
            if (stringArgs.isNotEmpty()) stringArgs.insertString(0, ", ")
            args.add(createStringStackArgument(i))
        }

        val addsTo = when(returnType) {
            RETURNVAL_INT, RETURNVAL_BOOLEAN -> ADDS_TO_NORMAL_STACK
            RETURNVAL_STRING -> ADDS_TO_STRING_STACK
            RETURNVAL_VOID -> DONT_PUSHES_TO_STACK
        }

        return FunctionNode(readableName, args, addsTo, hasNonStackDeps = true)
    }
}

enum class BotCommandReturnType {
    RETURNVAL_VOID,
    RETURNVAL_INT,
    RETURNVAL_BOOLEAN,
    RETURNVAL_STRING,
}