package at.nipe.playlegend.playlegendbans.shared.utils;

import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date utility to provide some bundled function for dates
 *
 * @author NoSleep - Nipe
 */
@UtilityClass
public class DateHelper {

  public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");

  public static String getFormattedDate(@Nonnull Date date) {
    return getFormattedDate(date, DATE_TIME_FORMAT);
  }

  public static String getFormattedDate(@Nonnull Date date, @Nonnull SimpleDateFormat format) {
    return format.format(date);
  }
}
