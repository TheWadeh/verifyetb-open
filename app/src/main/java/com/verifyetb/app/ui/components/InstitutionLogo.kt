package com.verifyetb.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifyetb.app.R
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.ui.theme.DarkBirrColors
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.TextPrimary

/**
 * Per-institution logos (bundled drawables). Every bank/wallet/cash id maps to its own asset.
 */
object InstitutionLogos {

    @DrawableRes
    fun drawableRes(institutionId: String): Int? = when (institutionId) {
        // Wallets
        "telebirr" -> R.drawable.logo_telebirr
        "cbe_birr" -> R.drawable.logo_cbe_birr
        "mpesa" -> R.drawable.logo_mpesa
        "kacha" -> R.drawable.logo_kacha
        "yaya" -> R.drawable.logo_yaya
        "ebirr" -> R.drawable.logo_ebirr
        "amole" -> R.drawable.logo_amole
        "hellocash" -> R.drawable.logo_hellocash
        "mbirr" -> R.drawable.logo_mbirr
        "coopay" -> R.drawable.logo_coopay
        "awashbirr" -> R.drawable.logo_awashbirr
        "owepay" -> R.drawable.logo_owepay
        "hibir" -> R.drawable.logo_hibir
        "cash" -> R.drawable.logo_cash
        // Banks
        "cbe" -> R.drawable.logo_cbe
        "awash" -> R.drawable.logo_awash
        "dashen" -> R.drawable.logo_dashen
        "boa" -> R.drawable.logo_boa
        "wegagen" -> R.drawable.logo_wegagen
        "united" -> R.drawable.logo_united
        "nib" -> R.drawable.logo_nib
        "coop" -> R.drawable.logo_coop
        "lion" -> R.drawable.logo_lion
        "zemen" -> R.drawable.logo_zemen
        "hibret" -> R.drawable.logo_hibret
        "bunna" -> R.drawable.logo_bunna
        "abay" -> R.drawable.logo_abay
        "addis" -> R.drawable.logo_addis
        "enat" -> R.drawable.logo_enat
        "debub" -> R.drawable.logo_debub
        "gadaa" -> R.drawable.logo_gadaa
        "hijra" -> R.drawable.logo_hijra
        "siinqee" -> R.drawable.logo_siinqee
        "tsehay" -> R.drawable.logo_tsehay
        "amhara" -> R.drawable.logo_amhara
        "ahadu" -> R.drawable.logo_ahadu
        "goh" -> R.drawable.logo_goh
        "shabelle" -> R.drawable.logo_shabelle
        "oromia" -> R.drawable.logo_oromia
        "anbesa" -> R.drawable.logo_anbesa
        "global" -> R.drawable.logo_global
        "dbe" -> R.drawable.logo_dbe
        else -> null
    }

    fun brandColor(institutionId: String): Color = when (institutionId) {
        "telebirr" -> Color(0xFF00ADEF)
        "cbe", "cbe_birr" -> Color(0xFF8B1A1A)
        "mpesa" -> Color(0xFF4CAF50)
        "kacha" -> Color(0xFFFF6B00)
        "amole" -> Color(0xFF1B4F9C)
        "hellocash" -> Color(0xFFE53935)
        "ebirr" -> Color(0xFF1565C0)
        "mbirr" -> Color(0xFF2E7D32)
        "yaya" -> Color(0xFF6A1B9A)
        "awash", "awashbirr" -> Color(0xFF0D47A1)
        "dashen" -> Color(0xFFB71C1C)
        "boa" -> Color(0xFFC62828)
        "wegagen" -> Color(0xFF00695C)
        "united" -> Color(0xFF283593)
        "nib" -> Color(0xFF4527A0)
        "coop", "coopay" -> Color(0xFF2E7D32)
        "lion" -> Color(0xFFF9A825)
        "zemen" -> Color(0xFFAD1457)
        "hibret" -> Color(0xFFEF6C00)
        "bunna" -> Color(0xFF5D4037)
        "abay" -> Color(0xFF0277BD)
        "enat" -> Color(0xFFD81B60)
        "amhara" -> Color(0xFFC62828)
        "oromia" -> Color(0xFF558B2F)
        "cash" -> DarkBirrColors.blue
        else -> Color(0xFF546E7A)
    }

    fun initials(displayName: String): String {
        val cleaned = displayName
            .replace(Regex("""(?i)\b(bank|international|of|the|wallet)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val parts = cleaned.split(' ').filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }
}

@Composable
fun InstitutionLogo(
    institutionId: String,
    displayName: String = EthiopianInstitutions.byId(institutionId)?.displayName ?: institutionId,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape((size.value * 0.28f).dp)
    val res = InstitutionLogos.drawableRes(institutionId)
    val brand = InstitutionLogos.brandColor(institutionId)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (res != null) Color.White else brand.copy(alpha = 0.16f))
            .border(1.dp, Divider.copy(alpha = 0.7f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (res != null) {
            Image(
                painter = painterResource(res),
                contentDescription = displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.08f),
            )
        } else {
            Text(
                text = InstitutionLogos.initials(displayName),
                color = if (institutionId == "cash") TextPrimary else brand,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.32f).sp,
                ),
            )
        }
    }
}

@Composable
fun InstitutionLogoByName(
    institutionName: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val inst = EthiopianInstitutions.byDisplayName(institutionName)
    InstitutionLogo(
        institutionId = inst?.id ?: institutionName.lowercase().replace(' ', '_'),
        displayName = inst?.displayName ?: institutionName,
        size = size,
        modifier = modifier,
    )
}
