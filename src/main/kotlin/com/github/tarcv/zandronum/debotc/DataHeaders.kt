package com.github.tarcv.zandronum.debotc

enum class DataHeaders(requiredArgs: Int = 0)
{
    DH_COMMAND(2) {
        override fun print(args: IntArray, vmState: VmState) {
            val command: BotCommand = toEnum(args[0])
            val expectedSecondArg = command.numArgs + command.numStringArgs
            if (expectedSecondArg != args[1]) {
                throw IllegalStateException(
                        "Second arg of DH_COMMAND (num of stack pops) is out of date:" +
                                " ${args[1]} != $expectedSecondArg for ${command.readableName}")
            }
            command.print(vmState)
        }
    },
    DH_STATEIDX,
    DH_STATENAME,
    DH_ONENTER,
    DH_MAINLOOP,
    DH_ONEXIT,
    DH_EVENT,
    DH_ENDONENTER,
    DH_ENDMAINLOOP,
    DH_ENDONEXIT,
    DH_ENDEVENT,
    DH_IFGOTO(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg1 = vmState.tryPopIntFromStack()
            if (arg1.startsWith("(") && arg1.endsWith(")") || !arg1.contains(" ")) {
                vmState.indentedPrintLn("if $arg1 goto label${args[0]};")
            } else {
                vmState.indentedPrintLn("if ($arg1) goto label${args[0]};")
            }
        }
    },
    DH_IFNOTGOTO(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg1 = vmState.tryPopIntFromStack()
            if (arg1.startsWith("(") && arg1.endsWith(")") || !arg1.contains(" ")) {
                vmState.indentedPrintLn("if (!$arg1) goto label${args[0]};")
            } else {
                vmState.indentedPrintLn("if (!($arg1)) goto label${args[0]};")
            }
        }
    },
    DH_GOTO(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("goto label${args[0]};")
        }
    },
    DH_ORLOGICAL {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 || $arg2)")
        }
    },
    DH_ANDLOGICAL {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 && $arg2)")
        }
    },
    DH_ORBITWISE {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 | $arg2)")
        }
    },
    DH_EORBITWISE {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 ^ $arg2)")
        }
    },
    DH_ANDBITWISE {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 & $arg2)")
        }
    },
    DH_EQUALS {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 == $arg2)")
        }
    },
    DH_NOTEQUALS {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 != $arg2)")
        }
    },
    DH_LESSTHAN {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 < $arg2)")
        }
    },
    DH_LESSTHANEQUALS {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 <= $arg2)")
        }
    },
    DH_GREATERTHAN {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 > $arg2)")
        }
    },
    DH_GREATERTHANEQUALS {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 >= $arg2)")
        }
    },
    DH_NEGATELOGICAL {
        override fun print(args: IntArray, vmState: VmState) {
            val arg1 = vmState.tryPopIntFromStack()
            if (arg1.startsWith("(") && arg1.endsWith(")") || !arg1.contains(" ")) {
                vmState.pushIntAsIs("!$arg1")
            } else {
                vmState.pushIntAsIs("(!$arg1)")
            }
        }
    },
    DH_LSHIFT {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 << $arg2)")
        }
    },
    DH_RSHIFT {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 >> $arg2)")
        }
    },
    DH_ADD {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 + $arg2)")
        }
    },
    DH_SUBTRACT {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 - $arg2)")
        }
    },
    DH_UNARYMINUS {
        override fun print(args: IntArray, vmState: VmState) {
            val arg1 = vmState.tryPopIntFromStack()
            if (arg1.startsWith("(") && arg1.endsWith(")") || !arg1.contains(" ")) {
                vmState.pushIntAsIs("-$arg1")
            } else {
                vmState.pushIntAsIs("-($arg1)")
            }
        }
    },
    DH_MULTIPLY {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 * $arg2)")
        }
    },
    DH_DIVIDE {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 / $arg2)")
        }
    },
    DH_MODULUS {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsIs("($arg1 % $arg2)")
        }
    },
    DH_PUSHNUMBER {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.pushIntAsIs(args[0])
        }
    },
    DH_PUSHSTRINGINDEX {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.pushStrAsIs("\"${vmState.strings[args[0]]}\"")
        }
    },
    DH_PUSHGLOBALVAR {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.pushIntAsTemporary("global${args[0]}")
        }
    },
    DH_PUSHLOCALVAR {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.pushIntAsTemporary("local${args[0]}")
        }
    },
    DH_DROPSTACKPOSITION {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("${vmState.tryPopIntFromStack()};")
        }
    },
    DH_SCRIPTVARLIST,
    DH_STRINGLIST,
    DH_INCGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]}++;")
        }
    },
    DH_DECGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]}--;")
        }
    },
    DH_ASSIGNGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]} = ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_ADDGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]} += ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_SUBGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]} -= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_MULGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]} *= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_DIVGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]} /= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_MODGLOBALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("global${args[0]} %= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_INCLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]}++;")
        }
    },
    DH_DECLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]}--;")
        }
    },
    DH_ASSIGNLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]} = ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_ADDLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]} += ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_SUBLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]} -= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_MULLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]} *= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_DIVLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]} /= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_MODLOCALVAR(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("local${args[0]} %= ${vmState.tryPopIntFromStack()};")
        }
    },
    DH_CASEGOTO(2) {
        override fun print(args: IntArray, vmState: VmState) {
            if (vmState.previousCommand != DH_CASEGOTO) {
                vmState.indentedPrintLn("switch (${vmState.tryPopIntFromStack()}) {")
            }
            vmState.indentedPrintLn("case ${args[0]}: goto label${args[1]};")
        }
    },
    DH_DROP {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("// '${vmState.tryPopIntFromStack()}' dropped from stack")
        }
    },
    DH_INCGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("globalArray${args[0]}[${vmState.tryPopIntFromStack()}]++;")
        }
    },
    DH_DECGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.indentedPrintLn("globalArray${args[0]}[${vmState.tryPopIntFromStack()}]--;")
        }
    },
    DH_ASSIGNGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("globalArray${args[0]}[$arg1] = $arg2;")
        }
    },
    DH_ADDGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("globalArray${args[0]}[$arg1] += $arg2;")
        }
    },
    DH_SUBGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("globalArray${args[0]}[$arg1] -= $arg2;")
        }
    },
    DH_MULGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("globalArray${args[0]}[$arg1] *= $arg2;")
        }
    },
    DH_DIVGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("globalArray${args[0]}[$arg1] /= $arg2;")
        }
    },
    DH_MODGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("globalArray${args[0]}[$arg1] %= $arg2;")
        }
    },
    DH_PUSHGLOBALARRAY(1) {
        override fun print(args: IntArray, vmState: VmState) {
            vmState.pushIntAsTemporary("globalArray${args[0]}[${vmState.tryPopIntFromStack()}]")
        }
    },
    DH_SWAP{
        override fun print(args: IntArray, vmState: VmState) {
            val arg2 = vmState.tryPopIntFromStack()
            val arg1 = vmState.tryPopIntFromStack()
            vmState.pushIntAsTemporary(arg2)
            vmState.pushIntAsTemporary(arg1)
        }
    },
    DH_DUP{
        override fun print(args: IntArray, vmState: VmState) {
            val arg1 = vmState.tryPopIntFromStack()
            val tempIndex = vmState.tempVarIndex.getAndIncrement()
            vmState.indentedPrintLn("temp$tempIndex = $arg1;")
            vmState.pushIntAsTemporary("temp$tempIndex")
            vmState.pushIntAsTemporary("temp$tempIndex")
        }
    },
    DH_ARRAYSET{
        override fun print(args: IntArray, vmState: VmState) {
            val highestValue = vmState.tryPopIntFromStack()
            val value = vmState.tryPopIntFromStack()
            val array = vmState.tryPopIntFromStack()
            vmState.indentedPrintLn("memset($array, $value, $highestValue)")
        }
    },

    NUM_DATAHEADERS;

    open fun print(args: IntArray, vmState: VmState) {
    }
}