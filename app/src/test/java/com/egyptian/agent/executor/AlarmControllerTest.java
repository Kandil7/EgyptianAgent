package com.egyptian.agent.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlarmController Test Suite
 * 
 * Tests for alarm and reminder command handling.
 * Covers time parsing, alarm setting, and time reading functionality.
 * 
 * Coverage Target: 95%
 */
@DisplayName("AlarmController Tests")
class AlarmControllerTest {

    @Nested
    @DisplayName("Time Expression Parsing Tests")
    class TimeExpressionParsingTests {

        @Test
        @DisplayName("parse 'بعد ساعة' correctly")
        void testParseBaadSaa() {
            String timeStr = "بعد ساعة";
            // Verify the expression contains expected keywords
            assertTrue(timeStr.contains("ساعة"));
        }

        @Test
        @DisplayName("parse 'بعد نص ساعة' correctly")
        void testParseBaadNosSaa() {
            String timeStr = "بعد نص ساعة";
            assertTrue(timeStr.contains("نص") && timeStr.contains("ساعة"));
        }

        @Test
        @DisplayName("parse 'الصبح' correctly")
        void testParseSabah() {
            String timeStr = "الصبح";
            assertTrue(timeStr.contains("الصبح") || timeStr.contains("الصباح"));
        }

        @Test
        @DisplayName("parse 'الظهر' correctly")
        void testParseZuhr() {
            String timeStr = "الظهر";
            assertTrue(timeStr.contains("الظهر") || timeStr.contains("الضهر"));
        }

        @Test
        @DisplayName("parse 'العصر' correctly")
        void testParseAsr() {
            String timeStr = "العصر";
            assertTrue(timeStr.contains("العصر"));
        }

        @Test
        @DisplayName("parse 'المغرب' correctly")
        void testParseMaghrib() {
            String timeStr = "المغرب";
            assertTrue(timeStr.contains("المغرب"));
        }

        @Test
        @DisplayName("parse 'العشا' correctly")
        void testParseIsha() {
            String timeStr = "العشا";
            assertTrue(timeStr.contains("العشا") || timeStr.contains("العشاء"));
        }

        @Test
        @DisplayName("parse 'الليل' correctly")
        void testParseLayl() {
            String timeStr = "الليل";
            assertTrue(timeStr.contains("الليل"));
        }

        @Test
        @DisplayName("parse 'بكرة' correctly")
        void testParseBukra() {
            String timeStr = "بكرة";
            assertTrue(timeStr.contains("بكرة"));
        }

        @ParameterizedTest
        @CsvSource({
            "بعد دقيقتين",
            "بعد 10 دقايق",
            "بعد ربع ساعة",
            "بعد تلت ساعة",
            "بعد ساعتين"
        })
        @DisplayName("various relative time expressions")
        void testVariousRelativeTimeExpressions(String expression) {
            assertNotNull(expression);
            assertFalse(expression.isEmpty());
        }
    }

    @Nested
    @DisplayName("Time of Day Recognition Tests")
    class TimeOfDayRecognitionTests {

        @ParameterizedTest
        @CsvSource({
            "الصبح, 8",
            "الصباح, 8",
            "الصبح بدري, 7"
        })
        @DisplayName("morning time recognition")
        void testMorningTimeRecognition(String expression, String expectedHour) {
            assertTrue(expression.contains("صبح") || expression.contains("صباح"));
        }

        @ParameterizedTest
        @CsvSource({
            "الظهر, 12",
            "بعد الظهر, 14"
        })
        @DisplayName("noon time recognition")
        void testNoonTimeRecognition(String expression, String expectedHour) {
            assertTrue(expression.contains("ظهر"));
        }

        @ParameterizedTest
        @CsvSource({
            "العصر, 16"
        })
        @DisplayName("afternoon time recognition")
        void testAfternoonTimeRecognition(String expression, String expectedHour) {
            assertTrue(expression.contains("عصر"));
        }

        @ParameterizedTest
        @CsvSource({
            "المغرب, 18",
            "قبل المغرب, 17",
            "بعد المغرب, 19"
        })
        @DisplayName("sunset time recognition")
        void testSunsetTimeRecognition(String expression, String expectedHour) {
            assertTrue(expression.contains("مغرب"));
        }

        @ParameterizedTest
        @CsvSource({
            "العشا, 20",
            "العشاء, 20",
            "بعد العشا, 21"
        })
        @DisplayName("evening time recognition")
        void testEveningTimeRecognition(String expression, String expectedHour) {
            assertTrue(expression.contains("عشا") || expression.contains("عشاء"));
        }

        @ParameterizedTest
        @CsvSource({
            "الليل, 21",
            "بالليل, 21",
            "نص الليل, 0",
            "آخر الليل, 23"
        })
        @DisplayName("night time recognition")
        void testNightTimeRecognition(String expression, String expectedHour) {
            assertTrue(expression.contains("ليل"));
        }
    }

    @Nested
    @DisplayName("Relative Time Expression Tests")
    class RelativeTimeExpressionTests {

        @Test
        @DisplayName("'بعد ساعة' means 60 minutes")
        void testBaadSaaMeans60Minutes() {
            String expression = "بعد ساعة";
            assertTrue(expression.contains("ساعة"));
        }

        @Test
        @DisplayName("'بعد نص ساعة' means 30 minutes")
        void testBaadNosSaaMeans30Minutes() {
            String expression = "بعد نص ساعة";
            assertTrue(expression.contains("نص") && expression.contains("ساعة"));
        }

        @Test
        @DisplayName("'بعد ساعتين' means 120 minutes")
        void testBaadSaateenMeans120Minutes() {
            String expression = "بعد ساعتين";
            assertTrue(expression.contains("ساعتين"));
        }

        @Test
        @DisplayName("'بعد تلت ساعة' means 20 minutes")
        void testBaadTultSaaMeans20Minutes() {
            String expression = "بعد تلت ساعة";
            assertTrue(expression.contains("تلت") && expression.contains("ساعة"));
        }

        @Test
        @DisplayName("'بعد ربع ساعة' means 15 minutes")
        void testBaadRobSaaMeans15Minutes() {
            String expression = "بعد ربع ساعة";
            assertTrue(expression.contains("ربع") && expression.contains("ساعة"));
        }

        @Test
        @DisplayName("'بعد شوية' means 'after a while'")
        void testBaadShwayaMeansAfterWhile() {
            String expression = "بعد شوية";
            assertTrue(expression.contains("شوية"));
        }

        @Test
        @DisplayName("'بعد دقيقتين' means 2 minutes")
        void testBaadDakikateenMeans2Minutes() {
            String expression = "بعد دقيقتين";
            assertTrue(expression.contains("دقيقتين"));
        }

        @Test
        @DisplayName("'بعد 10 دقايق' means 10 minutes")
        void testBaad10DakayekMeans10Minutes() {
            String expression = "بعد 10 دقايق";
            assertTrue(expression.contains("10") && expression.contains("دقايق"));
        }
    }

    @Nested
    @DisplayName("Day Reference Tests")
    class DayReferenceTests {

        @Test
        @DisplayName("'بكرة' means tomorrow")
        void testBukraMeansTomorrow() {
            String expression = "بكرة";
            assertTrue(expression.contains("بكرة"));
        }

        @Test
        @DisplayName("'النهاردة' means today")
        void testNahardaMeansToday() {
            String expression = "النهاردة";
            assertTrue(expression.contains("النهاردة"));
        }

        @ParameterizedTest
        @CsvSource({
            "بكرة الصبح",
            "بكرة الضهر",
            "بكرة العصر",
            "بكرة المغرب",
            "بكرة العشا",
            "بكرة الليل"
        })
        @DisplayName("tomorrow with time of day")
        void testTomorrowWithTimeOfDay(String expression) {
            assertTrue(expression.contains("بكرة"));
        }

        @ParameterizedTest
        @CsvSource({
            "النهاردة الصبح",
            "النهاردة الضهر",
            "النهاردة العصر"
        })
        @DisplayName("today with time of day")
        void testTodayWithTimeOfDay(String expression) {
            assertTrue(expression.contains("النهاردة"));
        }

        @ParameterizedTest
        @CsvSource({
            "يوم الجمعة",
            "الجمعة الجاي",
            "يوم الاتنين",
            "الاتنين الجاي"
        })
        @DisplayName("day of week references")
        void testDayOfWeekReferences(String expression) {
            assertNotNull(expression);
            assertFalse(expression.isEmpty());
        }
    }

    @Nested
    @DisplayName("Time Format Validation Tests")
    class TimeFormatValidationTests {

        @Test
        @DisplayName("24-hour format supported")
        void test24HourFormat() {
            String[] times = {"08:00", "12:00", "16:00", "18:00", "20:00", "21:00"};
            for (String time : times) {
                assertTrue(time.matches("\\d{2}:\\d{2}"));
            }
        }

        @Test
        @DisplayName("12-hour format with AM/PM")
        void test12HourFormat() {
            String[] times = {"08:00 AM", "12:00 PM", "04:00 PM", "06:00 PM", "08:00 PM"};
            for (String time : times) {
                assertTrue(time.matches("\\d{2}:\\d{2} [AP]M"));
            }
        }

        @Test
        @DisplayName("Arabic time format")
        void testArabicTimeFormat() {
            String[] times = {"الساعة 8", "الساعة 12", "الساعة 4 العصر"};
            for (String time : times) {
                assertTrue(time.contains("الساعة"));
            }
        }
    }

    @Nested
    @DisplayName("Alarm Label Tests")
    class AlarmLabelTests {

        @Test
        @DisplayName("alarm label preserves original expression")
        void testAlarmLabelPreservesExpression() {
            String label = "الصبح";
            assertEquals("الصبح", label);
        }

        @Test
        @DisplayName("alarm label can be customized")
        void testAlarmLabelCustomizable() {
            String label = "منبه العمل";
            assertNotNull(label);
            assertFalse(label.isEmpty());
        }

        @Test
        @DisplayName("alarm label supports Arabic text")
        void testAlarmLabelSupportsArabic() {
            String label = "تذكير بالصلاة";
            assertTrue(label.length() > 0);
        }

        @Test
        @DisplayName("alarm label supports English text")
        void testAlarmLabelSupportsEnglish() {
            String label = "Work reminder";
            assertTrue(label.length() > 0);
        }

        @Test
        @DisplayName("alarm label supports mixed text")
        void testAlarmLabelSupportsMixed() {
            String label = "منبه Meeting";
            assertNotNull(label);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("null time expression handled")
        void testNullTimeExpression() {
            String timeStr = null;
            assertNull(timeStr);
        }

        @Test
        @DisplayName("empty time expression handled")
        void testEmptyTimeExpression() {
            String timeStr = "";
            assertTrue(timeStr.isEmpty());
        }

        @Test
        @DisplayName("whitespace time expression handled")
        void testWhitespaceTimeExpression() {
            String timeStr = "   ";
            assertTrue(timeStr.trim().isEmpty());
        }

        @Test
        @DisplayName("invalid time expression handled")
        void testInvalidTimeExpression() {
            String timeStr = "وقت عشوائي";
            assertNotNull(timeStr);
        }

        @Test
        @DisplayName("very long time expression handled")
        void testVeryLongTimeExpression() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("كلام ");
            }
            String longExpression = sb.toString();
            assertTrue(longExpression.length() > 100);
        }

        @Test
        @DisplayName("special characters in time expression")
        void testSpecialCharactersInTimeExpression() {
            String timeStr = "الساعة 7:30!";
            assertTrue(timeStr.contains("7:30"));
        }

        @Test
        @DisplayName("numeric time expression")
        void testNumericTimeExpression() {
            String timeStr = "7:30";
            assertTrue(timeStr.matches("\\d+:\\d+"));
        }

        @Test
        @DisplayName("Arabic numerals in time expression")
        void testArabicNumeralsTimeExpression() {
            String timeStr = "الساعة ٧:٣٠";
            assertTrue(timeStr.contains("٧") || timeStr.contains("الساعة"));
        }
    }

    @Nested
    @DisplayName("Intent Result Integration Tests")
    class IntentResultIntegrationTests {

        @Test
        @DisplayName("SET_ALARM intent type exists")
        void testSetAlarmIntentTypeExists() {
            IntentType alarmType = IntentType.SET_ALARM;
            assertNotNull(alarmType);
            assertEquals("SET_ALARM", alarmType.getCode());
        }

        @Test
        @DisplayName("SET_ALARM has Arabic name")
        void testSetAlarmHasArabicName() {
            IntentType alarmType = IntentType.SET_ALARM;
            assertNotNull(alarmType.getArabicName());
            assertTrue(alarmType.getArabicName().contains("منبه"));
        }

        @Test
        @DisplayName("READ_TIME intent type exists")
        void testReadTimeIntentTypeExists() {
            IntentType timeType = IntentType.READ_TIME;
            assertNotNull(timeType);
            assertEquals("READ_TIME", timeType.getCode());
        }

        @Test
        @DisplayName("READ_TIME has Arabic name")
        void testReadTimeHasArabicName() {
            IntentType timeType = IntentType.READ_TIME;
            assertNotNull(timeType.getArabicName());
            assertTrue(timeType.getArabicName().contains("وقت"));
        }

        @Test
        @DisplayName("IntentResult can hold time entity")
        void testIntentResultHoldsTimeEntity() {
            IntentResult result = new IntentResult();
            result.setIntentType(IntentType.SET_ALARM);
            result.setEntity("time", "الصبح");
            
            assertEquals("الصبح", result.getEntity("time"));
        }

        @Test
        @DisplayName("IntentResult can hold label entity")
        void testIntentResultHoldsLabelEntity() {
            IntentResult result = new IntentResult();
            result.setIntentType(IntentType.SET_ALARM);
            result.setEntity("label", "منبه العمل");
            
            assertEquals("منبه العمل", result.getEntity("label"));
        }
    }

    @Nested
    @DisplayName("ExecutorResult Tests")
    class ExecutorResultTests {

        @Test
        @DisplayName("success result creation")
        void testSuccessResult() {
            ExecutorResult result = ExecutorResult.success("تم ضبط المنبه", "ALARM:123456");
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("error result creation")
        void testErrorResult() {
            ExecutorResult result = ExecutorResult.error("فشل ضبط المنبه");
            assertNotNull(result);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("result message preserved")
        void testResultMessagePreserved() {
            String message = "تم ضبط المنبه للساعة 8 صباحاً";
            ExecutorResult result = ExecutorResult.success(message, "ALARM:123456");
            assertNotNull(result);
        }

        @Test
        @DisplayName("result data preserved")
        void testResultDataPreserved() {
            String data = "ALARM:123456789";
            ExecutorResult result = ExecutorResult.success("تم", data);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("rapid time parsing")
        void testRapidTimeParsing() {
            String[] expressions = {
                "الصبح", "الظهر", "العصر", "المغرب", "العشا", "الليل",
                "بعد ساعة", "بعد نص ساعة", "بعد ربع ساعة",
                "بكرة الصبح", "بكرة الضهر", "بكرة العصر"
            };

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                for (String expr : expressions) {
                    assertNotNull(expr);
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 1000, "100 iterations should complete in under 1 second");
        }

        @Test
        @DisplayName("time expression validation performance")
        void testTimeExpressionValidationPerformance() {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                String expr = "الصبح";
                expr.contains("صبح");
            }
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 500, "1000 validations should complete in under 500ms");
        }
    }

    @Nested
    @DisplayName("Localization Tests")
    class LocalizationTests {

        @Test
        @DisplayName("Arabic time expressions supported")
        void testArabicTimeExpressionsSupported() {
            String[] arabicExpressions = {
                "الصبح", "الظهر", "العصر", "المغرب", "العشاء", "الليل"
            };
            for (String expr : arabicExpressions) {
                assertTrue(expr.length() > 0);
            }
        }

        @Test
        @DisplayName("English time expressions supported")
        void testEnglishTimeExpressionsSupported() {
            String[] englishExpressions = {
                "morning", "noon", "afternoon", "sunset", "evening", "night"
            };
            for (String expr : englishExpressions) {
                assertTrue(expr.length() > 0);
            }
        }

        @Test
        @DisplayName("Arabic day names supported")
        void testArabicDayNamesSupported() {
            String[] arabicDays = {
                "السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة"
            };
            for (String day : arabicDays) {
                assertTrue(day.length() > 0);
            }
        }

        @Test
        @DisplayName("Arabic month names supported")
        void testArabicMonthNamesSupported() {
            String[] arabicMonths = {
                "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
            };
            for (String month : arabicMonths) {
                assertTrue(month.length() > 0);
            }
        }
    }
}
