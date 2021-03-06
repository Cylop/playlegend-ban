package at.nipe.playlegend.playlegendbans.commands;

import at.nipe.playlegend.playlegendbans.BanFacade;
import at.nipe.playlegend.playlegendbans.context.ContextProperties;
import at.nipe.playlegend.playlegendbans.localization.MessageService;
import at.nipe.playlegend.playlegendbans.localization.Messages;
import at.nipe.playlegend.playlegendbans.parser.durationparser.DurationParser;
import at.nipe.playlegend.playlegendbans.shared.DurationPossibilities;
import at.nipe.playlegend.playlegendbans.shared.ExampleReasons;
import at.nipe.playlegend.playlegendbans.shared.exceptions.AccountNotFoundException;
import at.nipe.playlegend.playlegendbans.shared.exceptions.UnknownDurationUnitException;
import at.nipe.playlegend.playlegendbans.shared.resolution.Component;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static at.nipe.playlegend.playlegendbans.context.LocalePlaceholderHelper.*;

/**
 * Command that bans a player for a given duration and a message.
 *
 * <p>Usage: /ban player [duration in format 5w7d4h] [message or reason]</p>
 *
 * @author NoSleep - Nipe
 */
@Log
@Component
public class BanCommand implements CommandExecutor, TabCompleter {

  private final MessageService messageService;
  private final BanFacade banFacade;

  @Inject
  public BanCommand(@Nonnull MessageService messageService, @Nonnull BanFacade banFacade) {
    this.messageService = messageService;
    this.banFacade = banFacade;
  }

  @Override
  public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
    if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("playlegend.ban")) {
      sender.sendMessage(this.messageService.receive(Messages.ERRORS_NO_PERMISSION));
      return true;
    }

    // /ban <player> <duration> <message>
    if (args.length < 1) {
      sender.sendMessage(this.messageService.receive(Messages.ERRORS_BAN_ARGS_NOT_ENOUGH));
      return false;
    }

    var playerName = args[0];

    if(sender.getName().equals(playerName)) {
      sender.sendMessage(this.messageService.receive(
              ContextProperties.of(buildPlayerContext(sender)),
              Messages.ERRORS_BAN_SELF_BAN));
      return true;
    }

    var duration = "999y";
    if (args.length >= 2) {
      duration = args[1];
    }

    Date until;
    try {
      until = new DurationParser().parse(duration);
    } catch (UnknownDurationUnitException e) {
      sender.sendMessage(this.messageService.receive(
              ContextProperties.of(combine(buildPlayerContext(sender), buildAllowedUnitsContext())),
              Messages.ERRORS_DURATION_INVALID_UNIT));
      return true;
    }

    var message = this.messageService.receive(Messages.BAN_DEFAULT_REASON);

    if (args.length >= 3) {
      message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
      message = ChatColor.stripColor(message);
    }

    try {
      // args[0] -> player that should be banned
      var ban = this.banFacade.banPlayer(playerName, sender, until, message);

      var player = Bukkit.getPlayerExact(playerName);
      if(player != null) {
        player.kickPlayer(
                this.messageService.receive(ContextProperties.of(buildBanContext(ban)),
              Messages.BAN_MESSAGE));
      }
      sender.sendMessage(this.messageService.receive(ContextProperties.of(combine(buildPlayerContext(sender), buildTargetPlayerContext(playerName))), Messages.SUCCESS_BAN_SUCCESSFUL)
      );
      return true;
    } catch (SQLException e) {
      sender.sendMessage(
              this.messageService.receive(ContextProperties.of(buildPlayerContext(sender)),
              Messages.ERRORS_BAN_ERROR));
      log.log(Level.SEVERE, String.format("SQL Error occurred whilst banning player %s", playerName), e);
    } catch (AccountNotFoundException e) {
      sender.sendMessage(
              this.messageService.receive(ContextProperties.of(buildPlayerContext(e.getPlayerName())),
              Messages.ERRORS_USER_NO_ACCOUNT));
    }
    return false;
  }

  @Override
  public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, String[] args) {
    List<String> result = null;

    // args[0] -> player that should be banned
    // args[2] -> message or reason
   switch (args.length) {
     case 1 -> result = Bukkit.getOnlinePlayers().stream()
             .filter(player -> !player.getName().equals(sender.getName()) && player.getName().startsWith(args[0]))
             .map(Player::getName)
             .collect(Collectors.toList());
     case 2 -> result = DurationPossibilities.getAllowedIdentifiers();
     case 3 -> result = ExampleReasons.EXAMPLE_BAN_REASONS.stream().filter(reason -> reason.startsWith(args[2])).collect(Collectors.toList());
     default -> {}
   }
    return result;
  }
}
