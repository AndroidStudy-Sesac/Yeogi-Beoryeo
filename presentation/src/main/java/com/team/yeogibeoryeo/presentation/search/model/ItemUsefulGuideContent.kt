package com.team.yeogibeoryeo.presentation.search.model

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.presentation.R

data class ItemUsefulGuideContent(
    val type: ItemUsefulGuideType,
    @param:StringRes val labelResId: Int,
    @param:StringRes val titleResId: Int,
    @param:StringRes val descriptionResId: Int,
    @param:StringRes val detailResId: Int,
    @param:StringRes val cautionResId: Int,
    @param:StringRes val ctaResId: Int,
    val relatedSites: List<ItemUsefulGuideSite> = emptyList(),
)

data class ItemUsefulGuideSite(
    @param:StringRes val labelResId: Int,
    val url: String,
)

val itemUsefulGuideContents: List<ItemUsefulGuideContent> =
    listOf(
        ItemUsefulGuideContent(
            type = ItemUsefulGuideType.SMALL_E_WASTE,
            labelResId = R.string.item_useful_guide_small_e_waste_label,
            titleResId = R.string.item_useful_guide_small_e_waste_title,
            descriptionResId = R.string.item_useful_guide_small_e_waste_description,
            detailResId = R.string.item_useful_guide_small_e_waste_detail,
            cautionResId = R.string.item_useful_guide_small_e_waste_caution,
            ctaResId = R.string.item_useful_guide_small_e_waste_cta,
            relatedSites = listOf(
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_disposal_home,
                    url = DisposalHomeUrl,
                ),
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_pickup_locations,
                    url = ResourcePickupUrl,
                ),
            ),
        ),
        ItemUsefulGuideContent(
            type = ItemUsefulGuideType.REGIONAL_GUIDE,
            labelResId = R.string.item_useful_guide_regional_label,
            titleResId = R.string.item_useful_guide_regional_title,
            descriptionResId = R.string.item_useful_guide_regional_description,
            detailResId = R.string.item_useful_guide_regional_detail,
            cautionResId = R.string.item_useful_guide_regional_caution,
            ctaResId = R.string.item_useful_guide_regional_cta,
            relatedSites = listOf(
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_regional_map,
                    url = RegionalMapUrl,
                ),
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_regional_links,
                    url = RegionalLinksUrl,
                ),
            ),
        ),
        ItemUsefulGuideContent(
            type = ItemUsefulGuideType.REPRESENTATIVE_CATEGORY,
            labelResId = R.string.item_useful_guide_representative_label,
            titleResId = R.string.item_useful_guide_representative_title,
            descriptionResId = R.string.item_useful_guide_representative_description,
            detailResId = R.string.item_useful_guide_representative_detail,
            cautionResId = R.string.item_useful_guide_representative_caution,
            ctaResId = R.string.item_useful_guide_representative_cta,
            relatedSites = listOf(
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_disposal_methods,
                    url = DisposalMethodsUrl,
                ),
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_disposal_guidelines,
                    url = DisposalGuidelinesUrl,
                ),
            ),
        ),
        ItemUsefulGuideContent(
            type = ItemUsefulGuideType.ITEM_DICTIONARY,
            labelResId = R.string.item_useful_guide_dictionary_label,
            titleResId = R.string.item_useful_guide_dictionary_title,
            descriptionResId = R.string.item_useful_guide_dictionary_description,
            detailResId = R.string.item_useful_guide_dictionary_detail,
            cautionResId = R.string.item_useful_guide_dictionary_caution,
            ctaResId = R.string.item_useful_guide_dictionary_cta,
            relatedSites = listOf(
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_dictionary,
                    url = DictionaryUrl,
                ),
                ItemUsefulGuideSite(
                    labelResId = R.string.item_useful_guide_site_faq,
                    url = FaqUrl,
                ),
            ),
        ),
    )

fun ItemUsefulGuideType.toUsefulGuideContent(): ItemUsefulGuideContent =
    itemUsefulGuideContents.first { it.type == this }

private const val DisposalHomeUrl = "https://xn--oy2b29bd3a601b.kr/"
private const val DictionaryUrl = "https://xn--oy2b29bd3a601b.kr/front/dischargeMethod/dictionary.do"
private const val DisposalMethodsUrl =
    "https://xn--oy2b29bd3a601b.kr/front/dischargeMethod/typeItem.do?searchCnd=11"
private const val RegionalMapUrl = "https://xn--oy2b29bd3a601b.kr/front/region/location.do"
private const val RegionalLinksUrl = "https://xn--oy2b29bd3a601b.kr/front/support/bannerCollection.do"
private const val FaqUrl = "https://xn--oy2b29bd3a601b.kr/front/bbsList.do?bbsId=BBS_0002"
private const val DisposalGuidelinesUrl =
    "https://xn--oy2b29bd3a601b.kr/front/bbsList.do?bbsId=BBS_0003"
private const val ResourcePickupUrl = "https://www.re.or.kr/info/listPickupPage.do"
