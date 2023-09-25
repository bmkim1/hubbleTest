package side.side.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import side.side.domain.JsoupObject;
import side.side.domain.secured.SecuredKey;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {

    private static final Logger log = LogManager.getLogger(Util.class);

    public static boolean isEmpty(Object obj) {
        boolean result = true;
        if(obj != null) {
            if(obj instanceof String) {
                result = ((String)obj).replaceAll("\\s*\\t*\\r*\\n*","").isEmpty();
            } else if (obj instanceof String[]) {
                result = ((String[])obj).length == 0;
            } else if (obj instanceof Map<?,?>) {
                result = ((Map<?,?>)obj).isEmpty();
            } else if (obj instanceof Collection<?>) {
                result = ((Collection<?>)obj).isEmpty();
            } else {
                result = false;
            }
        }
        return result;
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static boolean isAnyEmpty(Object ...objs) {
        boolean empty = false;
        for (Object obj : objs) {
            empty &= isEmpty(obj);
        }
        return empty;
    }

    public static boolean isAllEmpty(Object ...objs) {
        boolean empty = false;
        for(Object obj : objs) {
            empty &= isEmpty(obj);

        }
        return empty;
    }

    public static boolean isNullZero(Number number) {
        return isEmpty(number) || number.doubleValue() == 0;
    }

    public static boolean isObjectClearlyNull(Object obj) {
        boolean isEmpty = true;

        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields) {
            try {
                if(field.getType().isEnum() || field.getType().getName().equals("boolean")) {
                    continue;
                }
                Method getter = obj.getClass().getMethod(toCamelCaseWithPrefix("get",field.getName()));

                SecuredKey securedKey = field.getAnnotation(SecuredKey.class);

                switch(field.getType().getName()) {
                    case "short":
                    case "int":
                    case "long":
                    case "float":
                    case "double":
                        if(securedKey != null) {
                            Number number = (Number) getter.invoke(obj);
                            switch(field.getType().getName()) {
                                case "short":
                                    isEmpty &= number.shortValue() == 0;
                                    break;
                                case "int":
                                    isEmpty &= number.intValue() == 0;
                                    break;
                                case "long":
                                    isEmpty &= number.longValue() == 0l;
                                    break;
                                case "float":
                                    isEmpty &= number.floatValue() == 0.0f;
                                    break;
                                case "double":
                                    isEmpty &= number.doubleValue() == 0.0;
                                    break;
                            }
                        }
                        break;
                    default:
                        if(field.getType().getName().startsWith("java.") || field.getType().getName().startsWith("com.antock.hubble2")) {
                            Object value = getter.invoke(obj);
                            isEmpty &= value == null;
                        }
                        break;
                }

                if(!isEmpty) {
                    break;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return isEmpty;
    }

    public static Double average(Double ...dValues) {
        Double result = null;

        double sum = 0;
        int cnt = 0;
        for(Double dValue : dValues) {
            if(dValue == null) {
                continue;
            }
            sum += dValue;
            cnt += 1;
        }

        result = sum / cnt;

        return result;
    }

    public static String eraseSpace(String str) {
        return str.replaceAll("\\s*\\t*\\r*\\n*", "");
    }


    public static String ifEmpty(String str, String defaultStr) {
        String result = str;

        if(isEmpty(result) || (isNotEmpty( result ) && result.equals("-")) ) {
            result = defaultStr;
        }

        return result;
    }


    public static boolean isNumber(String str) {
        boolean isNumber = false;
        try {
            Double.parseDouble(str);
            isNumber = true;
        } catch(Exception e) {}
        return isNumber;
    }

    public static Number getNumber(String str) {
        Number number = null;
        try {
            number = Double.parseDouble(str);
        } catch(Exception e) {}

        return number;
    }

    public static String cutCompanyName(String companyNameWithEnglishName) {
        String result = companyNameWithEnglishName;
        int index = companyNameWithEnglishName.indexOf(" (");
        if(index > -1) {
            result = companyNameWithEnglishName.substring(0, index);
        }
        return result;
    }

    public static String callJsoup(JsoupObject jObj) throws IOException {
        Connection conn = null;
        if(isEmpty(jObj.getDataKey()) || isEmpty(jObj.getDataValue())) {
            conn = Jsoup.connect(jObj.getSiteUrl())
                    .userAgent("Super Agent/0.0.1")
                    .header("content-type", "application/javascript; charset=utf-8")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;")
                    .ignoreContentType(true)
                    .timeout(10 * 1000);
        } else {
            conn = Jsoup.connect(jObj.getSiteUrl())
                    .userAgent("Super Agent/0.0.1")
                    .method(org.jsoup.Connection.Method.POST)
                    .data(jObj.getDataKey(), jObj.getDataValue())
                    .followRedirects(true)
                    .timeout(10 * 1000);
        }


        Connection.Response resp = conn.execute();

        return resp.body();
    }

    public static String encodeValue(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }


    public static void unzip(FileInputStream fis, String destDir, String destFileName) {
        byte[] buffer = new byte[1024];


        try(
                ZipInputStream zis = new ZipInputStream(fis, Charset.forName("euc-kr"));
        ) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(new File(destDir), destFileName, zipEntry);
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zipEntry = zis.getNextEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File newFile(File destinationDir, String destFileName, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, destFileName);

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    @SafeVarargs
    public static <T> Predicate<T> distinctByKeys(Function<? super T, ?>... keyExtractors) {
        final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();

        return t -> {
            final List<?> keys = Arrays.stream(keyExtractors)
                    .map(ke -> {
                        try {
                            ke.apply(t);
                        } catch(Exception e) {
                            log.error(ke);
                            log.error(t);
                            e.printStackTrace();
                        }
                        return ke.apply(t);
                    })
                    .collect(Collectors.toList());

            return seen.putIfAbsent(keys, Boolean.TRUE) == null;
        };
    }

    public static int getLeastCommonMultiple(int a, int b) {

        List<Integer> list = new ArrayList<>();
        int d = 2;
        while(a >= d && b>= d) {
            if (a % d == 0 && b % d == 0) {
                a /= d;
                b /= d;
                list.add(d);
                d = 2;
            } else {
                d++;
            }
        }

        int result = 1;
        for (int i : list) {
            result *= i;
        }
        result = result * a * b;

        return result;
    }

    public static void emptyToNull(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields) {
            try {
                String fieldName = field.getName();
                String getterName = String.format("get%s%s", fieldName.substring(0,1).toUpperCase(), fieldName.substring(1));

                if (fieldName.equals("idSeq") || fieldName.equals("crawlingSeq")) {
                    continue;
                }

                if (Util.isEmpty(obj.getClass().getMethod(getterName).invoke(obj))) {
                    field.setAccessible(true);
                    field.set(obj, null);
                }

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static String toCamelCaseWithPrefix(String prefix, String orgName) {
        return prefix + orgName.substring(0,1).toUpperCase() + orgName.substring(1);
    }

    public static String toCamelCaseFromSnakeCaseCaps(String orgName) {
        String[] splited = orgName.toLowerCase().split("");
        for (int i = 0; i < splited.length; i++) {
            if(splited[i].equals("_")) {
                splited[i+1] = splited[i+1].toUpperCase();
            }
        }
        return String.join("", splited).replace("_","");
    }

    public static String extractInRegex(String pattern, String str) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);

        String extractStr = "";
        if(m.find()) {
            log.debug(m.group(0));
            extractStr = m.group(0);
        }

        return extractStr;
    }

    public static float roundWithDigit(Double num, int digit) {
        if(Util.isEmpty(num)) {
            num = 0.0;
        }

        int digitVal = (int) Math.pow(10, digit);

        return (Math.round(num * digitVal) / Float.valueOf(digitVal));
    }

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String writeJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int indexOfOnList( List<?> list, Object obj ) {

        int rst = -1;

        if( isNotEmpty(list) && isNotEmpty(obj) ) {
            if( list.contains(obj) ) {
                rst = list.indexOf(obj);
            }
        }

        return rst;
    }

    public static String setEncodingFilename(String filename) {
        String encodedFilename = null;
        try {
            encodedFilename = URLEncoder.encode(filename, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedFilename;
    }

    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
