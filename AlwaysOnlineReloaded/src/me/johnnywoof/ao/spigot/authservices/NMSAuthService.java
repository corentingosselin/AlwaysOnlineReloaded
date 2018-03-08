package me.johnnywoof.ao.spigot.authservices;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import me.johnnywoof.ao.databases.Database;
import me.johnnywoof.ao.hybrid.AlwaysOnline;
import me.johnnywoof.ao.spigot.SpigotLoader;
import me.johnnywoof.ao.utils.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

public class NMSAuthService extends YggdrasilMinecraftSessionService {

	private final Database database;

	public NMSAuthService(YggdrasilAuthenticationService authenticationService, Database database) {
		super(authenticationService);
		this.database = database;
	}

	@Override
	public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {

		if (AlwaysOnline.MOJANG_OFFLINE_MODE) {

			UUID uuid = this.database.getUUID(user.getName());

			if (uuid != null) {

				return new GameProfile(uuid, user.getName());

			} else {

				SpigotLoader.getPlugin(SpigotLoader.class).log(Level.INFO, user.getName() + " " +
						"never joined this server before when mojang servers were online. Denying their access.");

				throw new AuthenticationUnavailableException("Mojang servers are offline and we can't authenticate the player with our own system.");

			}

		} else {

			return super.hasJoinedServer(user, serverId);

		}

	}


	private static Class<?> minecraftServer = Reflection.getMinecraftClass("MinecraftServer");
	private static Reflection.MethodInvoker getServer = Reflection.getMethod(minecraftServer,"getServer");
	private static Reflection.FieldAccessor authentificationService = Reflection.getField(minecraftServer,YggdrasilAuthenticationService.class,0);
	private static Reflection.FieldAccessor sessionService = Reflection.getField(minecraftServer,MinecraftSessionService.class,0);
	public static void setUp(SpigotLoader spigotLoader) throws Exception {
		Object ms = getServer.invoke(minecraftServer);
		sessionService.set(ms,new NMSAuthService((YggdrasilAuthenticationService) authentificationService.get(ms), spigotLoader.alwaysOnline.database) );
	}

}
