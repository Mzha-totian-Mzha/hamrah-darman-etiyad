package ir.mosafir.recovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mosafir.recovery.ui.theme.AmberAccent
import ir.mosafir.recovery.ui.theme.VioletPrimary

/**
 * کارت استاندارد بخش‌ها — سفید، خیلی‌گرد، سایهٔ نرم (بدون حاشیه) — سبک «پلنر من»
 * @param accent رنگ تیتر کارت — هر تب رنگ هویتی خودش را می‌فرستد (مثل «پلنر من»)
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color = VioletPrimary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            content()
        }
    }
}

/** کاشی آمار (عدد + برچسب) — سفید و سایه‌دار؛ مقدار و برچسب همیشه «یک‌خطی» */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // مقدار (مثل «۵/۵۰cc» یا «TDS شرب متادون») — حتی اگر بلند باشد، یک خط می‌ماند
            OneLineText(
                text = value,
                baseFontSize = 22,
                minFontSize = 10,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = VioletPrimary
            )
            // برچسب (مثل «پله جاری») — باز هم همیشه یک خط
            OneLineText(
                text = label,
                baseFontSize = 11,
                minFontSize = 7,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * متن همیشه «یک‌خطی» — اگر متن در عرض کاشی جا نشود، اندازه فونت
 * قدم‌به‌قدم کوچک می‌شود تا هرگز دوخطی نشود (بدون exception).
 */
@Composable
fun OneLineText(
    text: String,
    baseFontSize: Int,
    minFontSize: Int,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight? = null,
    color: androidx.compose.ui.graphics.Color,
    textAlign: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier
) {
    var fontSize by remember(text) { mutableStateOf(baseFontSize) }
    Text(
        text = text,
        style = style.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.25f).sp
        ),
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) fontSize = fontSize - 1
        }
    )
}

/** بنر یادآوری/توضیح با پس‌زمینه کهربایی — گرد و بدون حاشیه */
@Composable
fun NoticeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * سرتیتر صفحه‌ها — @param accent رنگ تیتر؛ هر تب رنگ اختصاصی خودش را دارد
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    accent: Color = VioletPrimary
) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** مُهر «ما می‌توانیم» */
@Composable
fun WeCanBadge() {
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.15f))
    ) {
        Text(
            text = "ما می‌توانیم ✋",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )
    }
}
