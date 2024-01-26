package net.walksantor.hextweaks.casting

import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import dan200.computercraft.shared.turtle.core.InteractDirection
import dan200.computercraft.shared.turtle.core.TurtleDropCommand
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

class ComputerMishapEnvironment(world: ServerLevel, player: ServerPlayer?,val env: ComputerCastingEnv) : MishapEnvironment(world,player) {
    override fun yeetHeldItemsTowards(targetPos: Vec3?) {
        if (env.turtleData != null) {
            val slot = env.turtleData.first.selectedSlot
            val item = env.turtleData.first.inventory.getItem(slot)
            env.turtleData.first.inventory.setItem(slot, ItemStack.EMPTY)
            val pos = env.turtleData.first.position.center
            val delta = targetPos!!.subtract(pos).normalize().scale(0.5)
            yeetItem(item,pos,delta)
        } else {
            val pos = env.caster!!.position()
            val delta = targetPos!!.subtract(pos).normalize().scale(0.5)

            for (hand in InteractionHand.entries) {
                val stack = env.caster!!.getItemInHand(hand)
                env.caster!!.setItemInHand(hand, ItemStack.EMPTY)
                this.yeetItem(stack, pos, delta)
            }
        }
    }

    override fun dropHeldItems() {
        if (env.turtleData != null) {
            env.turtleData.first.executeCommand(
                TurtleDropCommand(InteractDirection.FORWARD,64)
            )
        } else {
            this.yeetHeldItemsTowards(env.caster!!.position().add(env.caster!!.lookAngle ))
        }
    }

    override fun drown() {
        TODO("Not yet implemented")
    }

    override fun damage(healthProportion: Float) {
        TODO("Not yet implemented")
    }

    override fun removeXp(amount: Int) {
        TODO("Not yet implemented")
    }

    override fun blind(ticks: Int) {
        TODO("Not yet implemented")
    }
}