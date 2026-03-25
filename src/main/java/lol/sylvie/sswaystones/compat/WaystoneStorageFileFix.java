/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.compat;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class WaystoneStorageFileFix extends FileFix {
    public WaystoneStorageFileFix(Schema schema) {
        super(schema);
    }

    @Override
    public void makeFixer() {
        addFileFixOperation(FileFixOperations.move("data/sswaystones.dat", "data/sswaystones/waystones.dat"));
    }
}
