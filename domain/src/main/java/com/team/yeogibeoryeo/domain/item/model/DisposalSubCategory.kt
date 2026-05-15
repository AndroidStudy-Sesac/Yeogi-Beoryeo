package com.team.yeogibeoryeo.domain.item.model

enum class DisposalSubCategory(
    val displayName: String,
) {
    // 종이류
    NEWSPAPER("신문지"),
    CARDBOARD("골판지·박스"),
    BOOK_MAGAZINE("책·잡지"),
    PAPER_CUP("종이컵"),
    MILK_CARTON("우유팩"),
    PAPER_BAG("종이봉투"),

    // 유리류
    GLASS_BOTTLE("유리병"),
    GLASS_CONTAINER("유리컵·그릇"),

    // 금속류
    STEEL_CAN("철캔"),
    ALUMINUM_CAN("알루미늄캔"),
    METAL_CONTAINER("금속용기"),
    SCRAP_METAL("고철"),

    // 플라스틱류
    TRANSPARENT_PET_BOTTLE("투명페트병"),
    PET_BOTTLE("페트병"),
    PLASTIC_CONTAINER("플라스틱 용기"),

    // 전자·전기제품류
    LARGE_APPLIANCE("대형 가전"),
    SMALL_APPLIANCE("소형 가전"),
    BATTERY("건전지"),
    FLUORESCENT_LAMP("형광등"),
    MOBILE_PHONE("휴대폰"),

    // 의류·섬유류
    CLOTHING("의류"),
    SHOES("신발"),
    BAG("가방"),

    // 유해폐기물
    PAINT("페인트"),
    PESTICIDE("농약"),
    WASTE_OIL("폐유"),
}
