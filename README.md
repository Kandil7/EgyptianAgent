# Egyptian Agent - Revolutionizing Voice Assistance for Egypt

<p align="center">
  <img src="https://placehold.co/800x200/FF6B35/FFFFFF?text=الوكيل+المصري+-+Egyptian+Agent" alt="Egyptian Agent Banner">
</p>

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Egyptian Dialect](https://img.shields.io/badge/Dialect-Egyptian-blue.svg)](#)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25_Local-lightblue.svg)](#)

</div>

## 🇪🇬 The Vision: Empowering Egypt's Seniors and Visually Impaired

The Egyptian Agent is a revolutionary voice-controlled assistant designed specifically for Egyptian seniors and visually impaired users. Operating completely hands-free using voice commands in Egyptian dialect, it bridges the digital divide with cultural sensitivity and technological excellence.

### Our Mission
To create a truly inclusive technology that speaks the heart language of Egyptians - their own dialect - while respecting their privacy and independence.

## ✨ Key Features

### 🗣️ Egyptian Dialect Mastery
- **97.8% Accuracy**: Advanced Llama 3.2 3B model trained specifically for Egyptian Arabic
- **Cultural Context**: Understands expressions like "рен на Мама" and "фаитة عليа"
- **Natural Interaction**: Conversational commands in everyday Egyptian Arabic

### 👴 Senior-Focused Design
- **Senior Mode**: Slower, louder audio with simplified interactions
- **Emergency Response**: Automatic fall detection and emergency contact
- **Medication Reminders**: Automated scheduling for seniors
- **Large Button Mode**: Enhanced accessibility features

### 🔒 Privacy-First Architecture
- **100% Local Processing**: No data leaves your device
- **Offline Operation**: Works without internet connection
- **No Audio Storage**: Real-time processing with immediate deletion
- **Secure Wake Word**: Only listens for "يا صاحبي" and "يا كبير"

### 🏃‍♂️ System-Level Performance
- **Always Available**: Works even when screen is locked
- **Low Resource Usage**: Optimized for 6GB RAM devices (Honor X6c)
- **Battery Efficient**: <5% additional drain per hour
- **Root Access**: Deep system integration for seamless operation

## 🚀 Supported Commands

### Activation
- **Say "يا صاحبي"** to activate the assistant
- **In senior mode, say "يا كبير"** to activate

### Making Calls
- "اتصل بأمي" - Call mother
- "كلم بابا" - Call father
- "رن على ماما" - Call mother
- "اتصل بـ [name]" - Call any contact

### WhatsApp Messages
- "ابعت واتساب لـ [name]" - Send WhatsApp message
- "قول لـ [name] إن [message]" - Send specific message

### Setting Alarms
- "نبهني بكرة الصبح" - Set alarm for tomorrow morning
- "انبهني بعد ساعة" - Set alarm for 1 hour from now
- "ذكرني [time]" - Set reminder for specific time

### Emergency Features
- "يا نجدة" - Emergency call
- "استغاثة" - Distress call
- "مش قادر" - Emergency situation
- Triple volume button press - Emergency activation

## 🏗️ Technical Architecture

### AI-Powered Intelligence
Powered by cutting-edge technology:
- **Llama 3.2 3B Q4_K_M**: Advanced language model optimized for mobile
- **Whisper Egyptian ASR**: Speech-to-text specifically trained on Egyptian dialect
- **Hybrid Orchestrator**: Intelligent routing between processing methods
- **97.8% Accuracy**: Egyptian dialect understanding with cultural context

### Privacy & Security
- **Local Processing**: All AI runs on-device
- **No Data Transmission**: Zero data leaves the device
- **Encrypted Storage**: Secure model and data storage
- **Permission Controlled**: Minimal necessary permissions

### Device Optimization
Specifically optimized for Honor X6c (MediaTek Helio G81 Ultra):
- **Memory Management**: Optimized for 6GB RAM
- **Battery Efficiency**: Power-conscious design
- **Performance Tuning**: Device-specific optimizations
- **System Integration**: Deep Android integration

## 🛠️ Installation & Setup

### Prerequisites
- Honor X6c device (or compatible Android 12+ device)
- Unlocked bootloader
- Root access (Magisk)
- 2.5GB+ free storage for models

### Quick Setup
```bash
# Clone the repository
git clone https://github.com/your-org/egyptian-agent.git
cd egyptian-agent

# Initialize submodules
./initialize_submodules.sh

# Build the application
./build.sh --release --target honor-x6c

# Deploy to device
./deploy_production.sh
```

### Manual Installation
1. Unlock your device bootloader:
   ```bash
   adb reboot bootloader
   fastboot oem unlock
   ```

2. Install Magisk for root access:
   ```bash
   fastboot flash boot magisk_patched.img
   ```

3. Build and install as system app:
   ```bash
   ./gradlew assembleRelease
   adb push app/build/outputs/apk/release/EgyptianAgent-release.apk /sdcard/
   adb shell su -c "mkdir -p /system/priv-app/EgyptianAgent"
   adb shell su -c "cp /sdcard/EgyptianAgent-release.apk /system/priv-app/EgyptianAgent/"
   adb shell su -c "chmod 644 /system/priv-app/EgyptianAgent/EgyptianAgent-release.apk"
   ```

4. Reboot your device:
   ```bash
   adb reboot
   ```

## 📚 Documentation

All documentation is now organized in the [`docs/`](docs/) directory:

- [Architecture](docs/architecture/) - System design and architecture
- [Deployment](docs/deployment/) - Deployment guides and instructions
- [API Reference](docs/api/) - API documentation
- [User Guides](docs/guides/) - User manuals and troubleshooting
- [Testing](docs/testing/) - Test documentation
- [Performance](docs/performance/) - Performance benchmarks
- [Integration](docs/integration/) - Integration guides
- [Archive](docs/archive/) - Historical documents

### Quick Links

- [FunctionGemma Quick Start](docs/FUNCTIONGEMMA_QUICKSTART.md)
- [Deployment Guide](docs/deployment/DEPLOYMENT_GUIDE.md)
- [User Manual (Arabic)](docs/guides/user_manual_ar.md)
- [Troubleshooting](docs/guides/TROUBLESHOOTING.md)

- [Technical Documentation](documentation/technical_documentation.md) - Deep dive into architecture
- [User Manual (Arabic)](documentation/user_manual_ar.md) - Complete guide for end users
- [Installation Guide](documentation/installation_guide.md) - Step-by-step setup instructions
- [API Reference](ARCHITECTURE.md) - Technical API documentation

## 🧪 Testing & Quality

### Egyptian Dialect Testing
- Comprehensive test suite for Egyptian dialect understanding
- 97.8% accuracy validated with native speakers
- Cultural context and expression validation
- Elderly voice pattern testing

### Performance Benchmarks
- **Response Time**: 2.1s average end-to-end
- **Accuracy**: 97.8% Egyptian dialect understanding
- **Battery**: <5% additional drain per hour
- **Memory**: Optimized for 6GB RAM devices

## 🤝 Contributing

We welcome contributions to improve the Egyptian Agent! Whether you're fluent in Egyptian dialect, an Android expert, or passionate about accessibility, your contribution matters.

### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Areas Needing Contribution
- Egyptian dialect expansion
- Accessibility improvements
- Performance optimization
- Documentation translation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- The Egyptian community for inspiring this project
- Open-source AI community for Llama and Whisper models
- Android developers for accessibility frameworks
- The seniors and visually impaired users who deserve better technology

---

<div align="center">

**Made with ❤️ for the Egyptian community**

*Empowering voices, one command at a time.*

</div>