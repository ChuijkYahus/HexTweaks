package net.walksantor.hextweaks.computer

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import com.samsthenerd.duckyperiphs.hexcasting.utils.IotaLuaUtils
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.pocket.IPocketAccess
import dan200.computercraft.api.turtle.ITurtleAccess
import dan200.computercraft.api.turtle.TurtleSide
import net.minecraft.server.level.ServerLevel
import net.walksantor.hextweaks.casting.environment.ComputerCastingEnv

class WandPeripheral(val turtleData: Pair<ITurtleAccess,TurtleSide>?, val pocketData: IPocketAccess?) : IPeripheral {
    lateinit var vm: CastingVM
    var isInit = false

    override fun attach(computer: IComputerAccess?) {
        vm = CastingVM(CastingImage(), ComputerCastingEnv(turtleData,pocketData,getWorld(),computer!!))
        isInit = true
    }

    override fun detach(computer: IComputerAccess?) {
        isInit = false
    }


    fun getWorld(): ServerLevel {
        return if (turtleData==null) {
            pocketData!!.entity!!.level()
        } else {
            turtleData.first.level
        } as ServerLevel
    }

    override fun equals(other: IPeripheral?): Boolean = other is WandPeripheral

    override fun getType(): String = "wand"

    @LuaFunction
    fun getStack(): MethodResult {
        val world = getWorld()
        return MethodResult.of(vm.image.stack.map {IotaLuaUtils.getLuaObject(it,world)})
    }

    @LuaFunction
    fun pushStack(obj: Any) {
        val stack = vm.image.stack.toMutableList()
        stack.add(IotaLuaUtils.getIota(obj,getWorld()))
        vm.image = vm.image.copy(stack) // please petrak I am crying and begging. make the stack mutable
    }

    @LuaFunction
    fun popStack(): Any {
        if (vm.image.stack is MutableList) { //WRONG it can be EmptyList which causes errors
            val iota = (vm.image.stack as MutableList).removeLast()
            return IotaLuaUtils.getLuaObject(iota,getWorld())
        }
        return IotaLuaUtils.getLuaObject(NullIota(),getWorld())
    }

    @LuaFunction
    fun clearStack(): Int {
        if (vm.image.stack is MutableList) { //WRONG it can be EmptyList which causes errors
            val size = vm.image.stack.size
            (vm.image.stack as MutableList).clear()
            return size
        }
        return 0
    }

    @LuaFunction
    fun setStack(stack: List<Any>) {
        if (vm.image.stack is MutableList) {//WRONG it can be EmptyList which causes errors
            val world = getWorld()
            (vm.image.stack as MutableList).clear()
            (vm.image.stack as MutableList).addAll(stack.map { IotaLuaUtils.getIota(it,world)})
        }
    }

    @LuaFunction
    fun enlightened(): Boolean = vm.env.isEnlightened

    @LuaFunction
    fun getRavenmind(): Any {
        val nbt = vm.image.userData.getCompound(HexAPI.RAVENMIND_USERDATA)
        if (nbt != null) {
            val world = getWorld()
            val iota = IotaType.deserialize(nbt,world)
            return IotaLuaUtils.getLuaObject(iota,world)
        }
        return MethodResult.of(null)
    }

    @LuaFunction
    fun setRavenmind(iota: Any) {
        val newLocal = IotaLuaUtils.getIota(iota,getWorld())
        if (newLocal.type == HexIotaTypes.NULL)
            vm.image.userData.remove(HexAPI.RAVENMIND_USERDATA)
        else
            vm.image.userData.put(HexAPI.RAVENMIND_USERDATA, IotaType.serialize(newLocal))
    }

    @LuaFunction(mainThread = true)
    fun runPattern(args: IArguments) {
        val iota = when (args.count()) {
            0 -> PatternIota(HexActions.EVAL.prototype)
            1 -> IotaLuaUtils.getIota(args.get(0),getWorld())
            2 -> PatternIota(HexPattern.fromAngles(args.getString(1), HexDir.fromString(args.getString(0))))
            else -> GarbageIota()
        }
        val world = getWorld()
        if (vm.env.world != world) {
            vm = CastingVM(vm.image,ComputerCastingEnv(vm.env as ComputerCastingEnv,world))
        }
        vm.queueExecuteAndWrapIota(iota,getWorld())
    }

}