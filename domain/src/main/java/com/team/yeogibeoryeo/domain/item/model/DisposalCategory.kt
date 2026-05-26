package com.team.yeogibeoryeo.domain.item.model

enum class DisposalCategory(
    val displayName: String,
) {
    PAPER("종이류"),
    PAPER_PACK("종이팩"),
    COLORLESS_PET("무색페트병"),
    GLASS("유리류"),
    METAL("금속류"),
    PLASTIC("플라스틱류"),
    STYROFOAM("스티로폼류"),
    VINYL("비닐류"),
    FOOD_WASTE("음식물류"),
    LARGE_WASTE("대형폐기물"),
    ELECTRONICS("전자·전기제품류"),
    BATTERY("배터리"),
    LIGHTING("조명"),
    CLOTHING("의류·섬유류"),
    HAZARDOUS("유해폐기물"),
    NON_COMBUSTIBLE("불연성 폐기물"),
    CONSTRUCTION_WASTE("공사장 생활폐기물"),
    GENERAL("일반쓰레기"),
    OTHER("기타"),
}
