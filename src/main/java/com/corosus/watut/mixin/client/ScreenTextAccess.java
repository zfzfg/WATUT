package com.corosus.watut.mixin.client;

import java.util.function.Supplier;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class ScreenTextAccess {
  @Mixin(ChatScreen.class)
  public interface Chat {
    @Accessor("input")
    EditBox watut$input();
  }

  @Mixin(BookEditScreen.class)
  public interface Book {
    @Accessor("page")
    MultiLineEditBox watut$page();
  }

  @Mixin(AbstractSignEditScreen.class)
  public interface Sign {
    @Accessor("signField")
    TextFieldHelper watut$field();
  }

  @Mixin(AbstractCommandBlockEditScreen.class)
  public interface Command {
    @Accessor("commandEdit")
    EditBox watut$command();
  }

  @Mixin(TextFieldHelper.class)
  public interface Text {
    @Accessor("getMessageFn")
    Supplier<String> watut$message();
  }
}
