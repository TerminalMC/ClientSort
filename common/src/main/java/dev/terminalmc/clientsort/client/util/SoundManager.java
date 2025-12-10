/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.clientsort.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class SoundManager {

    private static long nextSoundTime = Long.MIN_VALUE;
    private static float pitch = 1.0F;
    private static float increment = 0.01F;
    private static @Nullable SoundInstance sound;

    public static boolean shouldPlaySortingSounds() {
        return options().playSoundSort && options().soundVolume > 0;
    }

    public static boolean shouldPlayOtherSounds() {
        return options().playSoundOther && options().soundVolume > 0;
    }

    /**
     * Resets the sound manager for a new operation.
     *
     * @param size the number of sounds to expect, for pitch interval calculation.
     */
    public static void resetForCount(int size) {
        increment = (options().soundPitchMax - options().soundPitchMin) / size;
        pitch = options().soundPitchMin;
    }

    /**
     * Plays an interaction sound with incremented pitch.
     */
    public static void play() {
        long now = Util.getMillis();
        float soundPitch = getPitch(); // Increment even if sound is skipped
        if (now >= nextSoundTime) {
            nextSoundTime = now + options().soundInterval;
            Identifier location = options().sortSoundLoc;
            if (location != null) {
                if (sound != null && !options().allowSoundOverlap) {
                    Minecraft.getInstance().getSoundManager().stop(sound);
                }
                sound = new SimpleSoundInstance(
                        location,
                        SoundSource.MASTER,
                        options().soundVolume,
                        soundPitch,
                        SoundInstance.createUnseededRandom(),
                        false,
                        0,
                        SoundInstance.Attenuation.NONE,
                        0,
                        0,
                        0,
                        true
                );
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        }
    }

    /**
     * @return an incremented pitch value.
     */
    private static float getPitch() {
        float val = pitch;
        pitch += increment;
        if (pitch > options().soundPitchMax) {
            pitch = options().soundPitchMax;
        }
        return val;
    }

    /**
     * Estimates the number of interaction sounds that will be played in the course of a sort
     * operation.
     */
    public static int estimateSortSounds(ItemStack[] stacks) {
        // Count non-empty stacks; assume all these require sorting
        int stackCount = 0;
        for (ItemStack stack : stacks) {
            if (stack != ItemStack.EMPTY) {
                stackCount++;
            }
        }
        // Count 'holes' that will require filling
        int compaction = 0;
        for (int i = 0; i < stackCount; i++) {
            if (stacks[i] == ItemStack.EMPTY) {
                compaction++;
            }
        }
        int size = stackCount + compaction;

        // Compensate for a small percentage of swaps requiring multiple clicks
        size += size / 15;

        return size;
    }

    /**
     * Estimates the number of interaction sounds that will be played in the course of a stack fill
     * operation.
     */
    public static int estimateStackFillSounds(ItemStack[] srcStacks, ItemStack[] dstStacks) {
        // Count partial stacks in the destination array
        int dstPartialCount = 0;
        for (ItemStack stack : dstStacks) {
            if (stack != ItemStack.EMPTY && stack.getCount() < stack.getMaxStackSize()) {
                dstPartialCount++;
            }
        }

        // Count non-empty slots in the source array
        int srcStackCount = 0;
        for (ItemStack stack : srcStacks) {
            if (stack != ItemStack.EMPTY) {
                srcStackCount++;
            }
        }

        // Assume a small percentage of the source stacks match the destination
        // partial stacks
        return (dstPartialCount / 2 + srcStackCount / 2) / 2;
    }

    /**
     * Estimates the number of interaction sounds that will be played in the course of a transfer
     * operation.
     */
    public static int estimateTransferSounds(ItemStack[] srcStacks, ItemStack[] dstStacks) {
        // Count non-empty slots in the source array
        int srcStackCount = 0;
        for (ItemStack stack : srcStacks) {
            if (stack != ItemStack.EMPTY) {
                srcStackCount++;
            }
        }

        // Count empty slots in the destination array
        int dstHoleCount = 0;
        for (ItemStack stack : dstStacks) {
            if (stack == ItemStack.EMPTY) {
                dstHoleCount++;
            }
        }

        // Compensate for a percentage of nonempty destination slots with
        // matching source stacks being only partially filled
        dstHoleCount += dstHoleCount / 8;

        // Smaller number defines length of operation
        return Math.min(srcStackCount, dstHoleCount);
    }
}
