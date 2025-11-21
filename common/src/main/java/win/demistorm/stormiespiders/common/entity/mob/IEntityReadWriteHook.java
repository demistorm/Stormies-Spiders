package win.demistorm.stormiespiders.common.entity.mob;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IEntityReadWriteHook {
	public void onRead(ValueInput input);

	public void onWrite(ValueOutput output);
}