package com.team.yeogibeoryeo.domain.item.model

enum class DisposalCategory(
    val displayName: String,
) {
    PAPER("종이류"),
    GLASS("유리류"),
    METAL("금속류"),
    PLASTIC("플라스틱류"),
    STYROFOAM("스티로폼류"),
    VINYL("비닐류"),
    FOOD_WASTE("음식물류"),
    LARGE_WASTE("대형폐기물"),
    ELECTRONICS("전자·전기제품류"),
    CLOTHING("의류·섬유류"),
    HAZARDOUS("유해폐기물"),
    GENERAL("일반쓰레기"),
    OTHER("기타"),
}
