package de.imdacro.economySystem.vault;

import de.imdacro.economySystem.EconomySystem;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import java.lang.reflect.*;
import java.util.UUID;

public class ReflectionVault {
  private final EconomySystem plugin;

  public ReflectionVault(EconomySystem plugin) {
    this.plugin = plugin;
    hook();
  }

  private void hook() {
    try {
      // Check whether Vault API is present
      Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
      Class<?> econRespClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
      Class<?> respTypeClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse$ResponseType");

      ServicesManager sm = this.plugin.getServer().getServicesManager();

      Object proxy = Proxy.newProxyInstance(econClass.getClassLoader(), new Class<?>[]{econClass}, (proxyObj, method, args) -> {
        String name = method.getName();
        try {
          switch (name) {
            case "getName":
              return plugin.getConfig().getString("economy.currency-name");
            case "isEnabled":
              return true;
            case "hasAccount": {
              String playerName = (String) args[0];
              UUID id = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
              return plugin.getDatabaseManager().accountExists(id.toString());
            }
            case "createPlayerAccount": {
              String playerName = (String) args[0];
              UUID id = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
              plugin.getDatabaseManager().createAccount(id.toString());
              return true;
            }
            case "getBalance": {
              String playerName = (String) args[0];
              UUID id = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
              return plugin.getDatabaseManager().getBalance(id.toString());
            }
            case "has": {
              String playerName = (String) args[0];
              double amount = ((Number) args[1]).doubleValue();
              UUID id = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
              return plugin.getDatabaseManager().getBalance(id.toString()) >= amount;
            }
            case "withdrawPlayer": {
              String playerName = (String) args[0];
              double amount = ((Number) args[1]).doubleValue();
              UUID id = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
              plugin.getDatabaseManager().removeBalance(id.toString(), amount);
              plugin.getDatabaseManager().createTransaction(id.toString(), "VAULT", amount);
              double balance = plugin.getDatabaseManager().getBalance(id.toString());
              Object response = createEconomyResponse(econRespClass, respTypeClass, amount, balance, "SUCCESS", "");
              return response;
            }
            case "depositPlayer": {
              String playerName = (String) args[0];
              double amount = ((Number) args[1]).doubleValue();
              UUID id = plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
              plugin.getDatabaseManager().addBalance(id.toString(), amount);
              plugin.getDatabaseManager().createTransaction("VAULT", id.toString(), amount);
              double balance = plugin.getDatabaseManager().getBalance(id.toString());
              Object response = createEconomyResponse(econRespClass, respTypeClass, amount, balance, "SUCCESS", "");
              return response;
            }
            default:
              Class<?> returnType = method.getReturnType();
              if (returnType == boolean.class || returnType == Boolean.class) return false;
              if (returnType == double.class || returnType == Double.class) return 0.0D;
              if (returnType == int.class || returnType == Integer.class) return 0;
              return null;
          }
        } catch (Throwable t) {
          t.printStackTrace();
          throw t;
        }
      });

      // Register the proxy as the Economy provider
      sm.register((Class) econClass, proxy, this.plugin, ServicePriority.High);

    } catch (ClassNotFoundException e) {
      // Vault not installed - do nothing
      plugin.getLogger().info("Vault not found, skipping Vault integration.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Object createEconomyResponse(Class<?> econRespClass, Class<?> respTypeClass, double amount, double balance, String respName, String errorMessage) throws Exception {
    Object respType = Enum.valueOf((Class) respTypeClass, respName);
    Constructor<?> ctor = econRespClass.getConstructor(double.class, double.class, respTypeClass, String.class);
    return ctor.newInstance(amount, balance, respType, errorMessage);
  }
}
