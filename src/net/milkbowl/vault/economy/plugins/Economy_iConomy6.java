/* This file is part of Vault.

    Vault is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Vault is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Vault.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.milkbowl.vault.economy.plugins;

import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.iCo6.Constants;
import com.iCo6.iConomy;
import com.iCo6.system.Accounts;
import com.iCo6.system.Holdings;

public class Economy_iConomy6 implements Economy {
    private static final Logger log = Logger.getLogger("Minecraft");

    private final String name = "iConomy 6";
    private JavaPlugin plugin = null;
    protected iConomy economy = null;
    private Accounts accounts;

    public Economy_iConomy6(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(new EconomyServerListener(this), plugin);
        // Load Plugin in case it was loaded before
        if (economy == null) {
            Plugin ec = plugin.getServer().getPluginManager().getPlugin("iConomy");
            if (ec != null && ec.isEnabled() && ec.getClass().getName().equals("com.iCo6.iConomy")) {
                economy = (iConomy) ec;
                accounts = new Accounts();
                log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), name));
            }
        }
    }

    public class EconomyServerListener implements Listener {
        Economy_iConomy6 economy = null;

        public EconomyServerListener(Economy_iConomy6 economy) {
            this.economy = economy;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginEnable(PluginEnableEvent event) {
            if (economy.economy == null) {
                Plugin ec = plugin.getServer().getPluginManager().getPlugin("iConomy");

                if (ec != null && ec.isEnabled() && ec.getClass().getName().equals("com.iCo6.iConomy")) {
                    economy.economy = (iConomy) ec;
                    accounts = new Accounts();
                    log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent event) {
            if (economy.economy != null) {
                if (event.getPlugin().getDescription().getName().equals("iConomy")) {
                    economy.economy = null;
                    log.info(String.format("[%s][Economy] %s unhooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        if (economy == null) {
            return false;
        } else {
            return economy.isEnabled();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String format(double amount) {
        return iConomy.format(amount);
    }

    @Override
    public String currencyNameSingular() {
        return Constants.Nodes.Major.getStringList().get(0);
    }

    @Override
    public String currencyNamePlural() {
        return Constants.Nodes.Major.getStringList().get(1);
    }

    @Override
    public double getBalance(String playerName) {
        if (accounts.exists(playerName))
            return accounts.get(playerName).getHoldings().getBalance();
        else
            return 0;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        Holdings holdings = accounts.get(playerName).getHoldings();
        if (holdings.hasEnough(amount)) {
            holdings.subtract(amount);
            return new EconomyResponse(amount, holdings.getBalance(), ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, holdings.getBalance(), ResponseType.FAILURE, "Insufficient funds");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        Holdings holdings = accounts.get(playerName).getHoldings();
        holdings.add(amount);
        return new EconomyResponse(amount, holdings.getBalance(), ResponseType.SUCCESS, null);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        if (accounts.exists(name)) {
            return new EconomyResponse(0, accounts.get(name).getHoldings().getBalance(), ResponseType.FAILURE, "That account already exists.");
        }
        boolean created = accounts.create(name);
        if (created) {
            return new EconomyResponse(0, 0, ResponseType.SUCCESS, "");
        } else { 
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "There was an error creating the account");
        }

    }

    @Override
    public EconomyResponse deleteBank(String name) {
        if (accounts.exists(name)) {
            accounts.remove(name);
            return new EconomyResponse(0, 0, ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "That bank account does not exist.");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        if (has(name, amount)) {
            return new EconomyResponse(0, amount, ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(0, accounts.get(name).getHoldings().getBalance(), ResponseType.FAILURE, "The account does not have enough!");
        }
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return withdrawPlayer(name, amount);
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return depositPlayer(name, amount);
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "iConomy 6 does not support Bank owners.");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "iConomy 6 does not support Bank members.");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        if (!accounts.exists(name)) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "There is no bank account with that name");
        } else {
            return new EconomyResponse(0, accounts.get(name).getHoldings().getBalance(), ResponseType.SUCCESS, null);
        }
    }

    @Override
    public List<String> getBanks() {
        throw new UnsupportedOperationException("iConomy does not support listing of bank accounts");
    }

    @Override
    public boolean hasBankSupport() {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName) {
        return accounts.exists(playerName);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        if (hasAccount(playerName)) {
            return false;
        }
        return accounts.create(playerName);
    }
}
