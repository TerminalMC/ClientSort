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

package dev.terminalmc.clientsort.util;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import static dev.terminalmc.clientsort.config.Config.options;

public class SoundUtil {
    private static long time = Long.MIN_VALUE;
    private static float pitch = 1.0F;
    private static float increment = 0.01F;
    private static @Nullable SoundInstance sound;
    
    public static void reset(int size) {
        increment = (options().soundMaxPitch - options().soundMinPitch) / size;
        pitch = options().soundMinPitch;
    }

    private static float pitch() {
        float val = pitch;
        pitch += increment;
        if (pitch > options().soundMaxPitch) {
            pitch = options().soundMaxPitch;
        }
        return val;
    }
    
    public static void play() {
        long now = Util.getMillis();
        float soundPitch = pitch(); // Increment even if sound is skipped
        if (now >= time) {
            time = now + options().soundRate;
            ResourceLocation location = options().sortSoundLoc;
            if (location != null) {
                if (sound != null) {
                    Minecraft.getInstance().getSoundManager().stop(sound);
                }
                sound = new SimpleSoundInstance(
                        location, SoundSource.MASTER, options().soundVolume, soundPitch,
                        SoundInstance.createUnseededRandom(), false, 0,
                        SoundInstance.Attenuation.NONE, 0, 0, 0, true);
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        }
    }
}
