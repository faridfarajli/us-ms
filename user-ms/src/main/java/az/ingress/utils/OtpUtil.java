package az.ingress.utils;

import java.text.DecimalFormat;
import java.util.Random;

public class OtpUtil {
    public static String getRandomOTP(){
        String otp= new DecimalFormat("000000").format(new Random().nextInt(999999));
        return otp;
    }
}
