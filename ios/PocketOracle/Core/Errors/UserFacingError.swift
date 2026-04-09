import Foundation

enum UserFacingErrorContext {
    case astrologyLoad
    case astrologyGenerate
    case tarot
    case bazi
    case home
}

enum UserFacingErrorMapper {

    static func message(for error: Error, context: UserFacingErrorContext) -> String {
        if let error = error as? AstrologyEngineError {
            switch error {
            case .invalidBirthTime:
                return AppLanguageOption.isChinese
                    ? "出生时间看起来不太对，请重新确认日期和时间。"
                    : "The birth time looks invalid. Please check the date and time and try again."
            case .missingTimeZone:
                return AppLanguageOption.isChinese
                    ? "当前城市的时区资料暂时不可用，请换一个城市后再试。"
                    : "The selected city's time zone data is unavailable right now. Please try another city."
            case .missingSigns:
                return message(forMissingContentIn: context)
            }
        }

        if error is ContentLoaderError || error is DecodingError || error is BaziError {
            return message(forMissingContentIn: context)
        }

        return fallbackMessage(for: context)
    }

    static func log(_ error: Error, context: UserFacingErrorContext) {
        #if DEBUG
        print("[\(context.label)] \(error)")
        #endif
    }

    private static func message(forMissingContentIn context: UserFacingErrorContext) -> String {
        switch context {
        case .astrologyLoad, .astrologyGenerate:
            return AppLanguageOption.isChinese
                ? "本命盘资料暂时没有加载成功，请重新打开应用后再试。"
                : "Natal chart data could not be loaded right now. Please reopen the app and try again."
        case .tarot:
            return AppLanguageOption.isChinese
                ? "塔罗资料暂时没有加载成功，请稍后再试。"
                : "Tarot data could not be loaded right now. Please try again shortly."
        case .bazi:
            return AppLanguageOption.isChinese
                ? "八字资料暂时没有加载成功，请稍后再试。"
                : "Bazi data could not be loaded right now. Please try again shortly."
        case .home:
            return AppLanguageOption.isChinese
                ? "今日指引暂时没有加载成功，请稍后再试。"
                : "Today's guidance could not be loaded right now. Please try again shortly."
        }
    }

    private static func fallbackMessage(for context: UserFacingErrorContext) -> String {
        switch context {
        case .astrologyLoad:
            return AppLanguageOption.isChinese
                ? "本命盘资料暂时不可用，请稍后再试。"
                : "Natal chart data is temporarily unavailable. Please try again later."
        case .astrologyGenerate:
            return AppLanguageOption.isChinese
                ? "暂时无法生成本命盘，请稍后再试。"
                : "We couldn't generate the natal chart right now. Please try again later."
        case .tarot:
            return AppLanguageOption.isChinese
                ? "暂时无法完成这次抽牌，请稍后再试。"
                : "We couldn't complete this tarot draw right now. Please try again later."
        case .bazi:
            return AppLanguageOption.isChinese
                ? "暂时无法生成八字解读，请稍后再试。"
                : "We couldn't generate the Bazi reading right now. Please try again later."
        case .home:
            return AppLanguageOption.isChinese
                ? "暂时无法加载首页内容，请稍后再试。"
                : "We couldn't load the home content right now. Please try again later."
        }
    }
}

private extension UserFacingErrorContext {
    var label: String {
        switch self {
        case .astrologyLoad: return "AstrologyLoad"
        case .astrologyGenerate: return "AstrologyGenerate"
        case .tarot: return "Tarot"
        case .bazi: return "Bazi"
        case .home: return "Home"
        }
    }
}
