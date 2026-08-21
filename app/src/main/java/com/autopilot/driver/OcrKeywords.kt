package com.autopilot.driver

object OcrKeywords {
    val ACCEPT_KEYWORDS = listOf(
        // English
        "accept", "Accept", "Accept Ride", "Accept Now", "New Ride", "ACCEPT", "Tap to Accept",
        "Accept Trip", "Confirm", "CONFIRM", "Take Ride", "Pick Up",

        // Hindi
        "स्वीकार करें", "स्वीकार करे", "स्वीकार", "राइड स्वीकार करें", "नई राइड", "स्वीकारें",
        "स्वीकृत करें", "मंजूर करें",

        // Gujarati
        "સ્વીકાર કરો", "સ્વીકાર", "રાઇડ સ્વીકારો", "નવી રાઇડ", "મંજૂર કરો",

        // Marathi
        "स्वीकार करा", "राइड स्वीकारा", "नवीन राइड", "स्वीकार", "मंजूर करा",

        // Bengali
        "স্বীকার করুন", "স্বীকার", "রাইড গ্রহণ করুন", "নতুন রাইড", "গ্রহণ করুন",

        // Telugu
        "అంగీకరించండి", "స్వీకరించండి", "రైడ్ అంగీకరించండి", "కొత్త రైడ్", "ఆమోదించండి",

        // Tamil
        "ஏற்றுக்கொள்", "அங்கீகரிக்கவும்", "ரைடு ஏற்கவும்", "புதிய ரைடு", "ஏற்கவும்",

        // Kannada
        "ಸ್ವೀಕರಿಸಿ", "ಅಂಗೀಕರಿಸಿ", "ರೈಡ್ ಸ್ವೀಕರಿಸಿ", "ಹೊಸ ರೈಡ್", "ಒಪ್ಪಿಕೊಳ್ಳಿ",

        // Malayalam
        "സ്വീകരിക്കുക", "അംഗീകരിക്കുക", "റൈഡ് സ്വീകരിക്കുക", "പുതിയ റൈഡ്", "അംഗീകരിക്കുക",

        // Punjabi
        "ਮਨਜ਼ੂਰ ਕਰੋ", "ਸਵੀਕਾਰ ਕਰੋ", "ਰਾਈਡ ਮਨਜ਼ੂਰ ਕਰੋ", "ਨਵੀਂ ਰਾਈਡ", "ਕਬੂਲ ਕਰੋ"
    )

    val PRICE_PATTERNS = listOf(
        Regex("[₹Rs.\s]*([0-9,]+(?:\.[0-9]{1,2})?)"),
        Regex("([0-9,]+)\s*₹"),
        Regex("fare\s*:?\s*([0-9,]+)", RegexOption.IGNORE_CASE),
        Regex("price\s*:?\s*([0-9,]+)", RegexOption.IGNORE_CASE),
        Regex("amount\s*:?\s*([0-9,]+)", RegexOption.IGNORE_CASE),
        Regex("\₹\s*([0-9,]+)")
    )
}
