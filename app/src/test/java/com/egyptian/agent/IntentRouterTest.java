package com.egyptian.agent;

import com.egyptian.agent.nlp.IntentRouter;
import com.egyptian.agent.nlp.IntentType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for IntentRouter
 */
public class IntentRouterTest {

    @Test
    public void testDetectEmergencyIntent() {
        // Test emergency intent detection
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("يا نجدة"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("استغاثة"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("طوارئ"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("إسعاف"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("help"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("emergency"));
    }

    @Test
    public void testDetectCallIntent() {
        // Test call intent detection
        assertEquals(IntentType.CALL_CONTACT, IntentRouter.detectIntent("اتصل بأمي"));
        assertEquals(IntentType.CALL_CONTACT, IntentRouter.detectIntent("كلم بابا"));
        assertEquals(IntentType.CALL_CONTACT, IntentRouter.detectIntent("رن على ماما"));
        assertEquals(IntentType.CALL_CONTACT, IntentRouter.detectIntent("call mom"));
        assertEquals(IntentType.CALL_CONTACT, IntentRouter.detectIntent("ring dad"));
    }

    @Test
    public void testDetectWhatsAppIntent() {
        // Test WhatsApp intent detection
        assertEquals(IntentType.SEND_WHATSAPP, IntentRouter.detectIntent("ابعت واتساب لباسم"));
        assertEquals(IntentType.SEND_WHATSAPP, IntentRouter.detectIntent("قول لأحمد إن جاى"));
        assertEquals(IntentType.SEND_WHATSAPP, IntentRouter.detectIntent("send whatsapp"));
        assertEquals(IntentType.SEND_WHATSAPP, IntentRouter.detectIntent("message"));
    }

    @Test
    public void testDetectAlarmIntent() {
        // Test alarm intent detection
        assertEquals(IntentType.SET_ALARM, IntentRouter.detectIntent("انبهني بكرة"));
        assertEquals(IntentType.SET_ALARM, IntentRouter.detectIntent("نبهني بعد ساعة"));
        assertEquals(IntentType.SET_ALARM, IntentRouter.detectIntent("ذكرني"));
        assertEquals(IntentType.SET_ALARM, IntentRouter.detectIntent("set alarm"));
        assertEquals(IntentType.SET_ALARM, IntentRouter.detectIntent("remind me"));
    }

    @Test
    public void testDetectTimeIntent() {
        // Test time intent detection
        assertEquals(IntentType.READ_TIME, IntentRouter.detectIntent("الوقت كام"));
        assertEquals(IntentType.READ_TIME, IntentRouter.detectIntent("الساعه كام"));
        assertEquals(IntentType.READ_TIME, IntentRouter.detectIntent("what time is it"));
        assertEquals(IntentType.READ_TIME, IntentRouter.detectIntent("time"));
    }

    @Test
    public void testDetectMissedCallsIntent() {
        // Test missed calls intent detection
        assertEquals(IntentType.READ_MISSED_CALLS, IntentRouter.detectIntent("قولي الفايتة"));
        assertEquals(IntentType.READ_MISSED_CALLS, IntentRouter.detectIntent("شوفلي المكالمات الفايتة"));
        assertEquals(IntentType.READ_MISSED_CALLS, IntentRouter.detectIntent("check missed calls"));
        assertEquals(IntentType.READ_MISSED_CALLS, IntentRouter.detectIntent("look for missed"));
    }

    @Test
    public void testDetectUnknownIntent() {
        // Test unknown intent detection
        assertEquals(IntentType.UNKNOWN, IntentRouter.detectIntent("كلمه مجهول"));
        assertEquals(IntentType.UNKNOWN, IntentRouter.detectIntent("something random"));
        assertEquals(IntentType.UNKNOWN, IntentRouter.detectIntent(""));
        assertEquals(IntentType.UNKNOWN, IntentRouter.detectIntent(" "));
    }

    @Test
    public void testDetectIntentWithMixedPhrases() {
        // Test intent detection with mixed Egyptian dialect phrases
        assertEquals(IntentType.CALL_CONTACT, IntentRouter.detectIntent("عايز اتكلم مع ماما دلوقتي"));
        assertEquals(IntentType.SEND_WHATSAPP, IntentRouter.detectIntent("بدي ابعت رسالة لباسم بكرة"));
        assertEquals(IntentType.SET_ALARM, IntentRouter.detectIntent("خلي بالك انبهني بكرة الصبح"));
    }

    @Test
    public void testDetectEmergencyWithEgyptianVariations() {
        // Test emergency detection with Egyptian dialect variations
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("يا نجدة بسرعة"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("في حد يجي"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("مش قادر"));
        assertEquals(IntentType.EMERGENCY, IntentRouter.detectIntent("ساعتووني"));
    }
}