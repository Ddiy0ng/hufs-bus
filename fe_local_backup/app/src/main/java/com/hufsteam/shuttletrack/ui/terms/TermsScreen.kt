package com.hufsteam.shuttletrack.ui.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.theme.DividerColor
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

enum class TermsType { PRIVACY, SERVICE }

private val BorderColor = Color(0xFFCCCCCC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(type: TermsType, onBack: () -> Unit) {
    val title = when (type) {
        TermsType.PRIVACY -> "개인정보 수집 및 이용"
        TermsType.SERVICE -> "서비스 이용 약관"
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { Text(title, color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Color.White)
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (type) {
                TermsType.PRIVACY -> PrivacyContent()
                TermsType.SERVICE -> ServiceContent()
            }
        }
    }
}

@Composable
private fun PrivacyContent() {
    SectionTitle("개인정보 수집 및 이용 안내")
    Spacer(Modifier.height(8.dp))
    BodyText("본 서비스는 한국외국어대학교 셔틀버스 실시간 위치 및 운행 정보 제공 서비스를 위해 아래와 같이 개인정보를 수집·이용합니다.")
    Spacer(Modifier.height(16.dp))

    // 테두리 있는 표
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor)
    ) {
        // 헤더 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(Color(0xFFF0F4FA))
        ) {
            Text(
                "수집 항목",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1.2f)
                    .padding(8.dp)
            )
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
            Text(
                "수집 목적",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1.5f)
                    .padding(8.dp)
            )
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
            Text(
                "보유 및 이용 기간",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1.3f)
                    .padding(8.dp)
            )
        }

        // 구분선
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

        // 데이터 행 1
        PrivacyTableRow(
            item    = "학교 이메일\n(@hufs.ac.kr)",
            purpose = "회원 식별 및 학교 구성원 인증",
            period  = "회원 탈퇴 시까지"
        )

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

        // 데이터 행 2
        PrivacyTableRow(
            item    = "비밀번호",
            purpose = "로그인 및 계정 보호",
            period  = "회원 탈퇴 시까지"
        )

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

        // 데이터 행 3
        PrivacyTableRow(
            item    = "즐겨찾기 정보 및 서비스 이용 기록",
            purpose = "맞춤형 서비스 제공 및 기능 개선",
            period  = "회원 탈퇴 시까지"
        )
    }

    Spacer(Modifier.height(16.dp))
    NoteText("※ 본 서비스는 한국외국어대학교 구성원 인증을 위해 학교 이메일 도메인(@hufs.ac.kr)만 가입이 가능합니다.")
    Spacer(Modifier.height(6.dp))
    NoteText("※ 이메일 인증은 Gmail SMTP 서비스를 활용하여 진행됩니다.")
    Spacer(Modifier.height(6.dp))
    NoteText("※ 이용자는 개인정보 수집 및 이용에 대한 동의를 거부할 권리가 있습니다. 다만, 동의를 거부할 경우 회원가입 및 서비스 이용이 제한될 수 있습니다.")
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun PrivacyTableRow(item: String, purpose: String, period: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Text(
            item,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1.2f)
                .padding(8.dp)
        )
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
        Text(
            purpose,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1.5f)
                .padding(8.dp)
        )
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
        Text(
            period,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1.3f)
                .padding(8.dp)
        )
    }
}

@Composable
private fun ServiceContent() {
    SectionTitle("서비스 이용약관")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제1조 (목적)")
    BodyText("본 약관은 한국외국어대학교 셔틀버스 실시간 위치 서비스(이하 서비스)의 이용과 관련하여 서비스 제공자와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제2조 (회원가입)")
    BodyText("1. 회원가입은 한국외국어대학교 이메일(@hufs.ac.kr) 인증을 완료한 경우에만 가능합니다.")
    BodyText("2. 이용자는 정확한 정보를 입력하여야 하며, 타인의 정보를 도용하여 가입할 수 없습니다.")
    BodyText("3. 서비스 운영자는 허위 정보 등록 또는 부정 이용이 확인될 경우 회원 자격을 제한하거나 삭제할 수 있습니다.")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제3조 (서비스 내용)")
    BodyText("서비스는 다음 기능을 제공합니다.")
    BulletText("셔틀버스 실시간 위치 조회")
    BulletText("정류장별 도착 예정 정보 제공")
    BulletText("운행 시간표 조회")
    BulletText("즐겨찾기 기능")
    BulletText("기타 셔틀 운영 관련 서비스")
    BodyText("단, 시스템 점검·통신 장애·학교 운영 상황 등에 따라 서비스 제공이 일시적으로 제한될 수 있습니다.")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제4조 (이용자의 의무)")
    BodyText("이용자는 다음 행위를 해서는 안 됩니다.")
    BodyText("1. 타인의 계정 도용")
    BodyText("2. 서비스 운영 방해 행위")
    BodyText("3. 비정상적인 접근 또는 서버 공격 행위")
    BodyText("4. 허위 정보 입력")
    BodyText("5. 기타 관계 법령에 위반되는 행위")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제5조 (서비스 제한)")
    BodyText("서비스 운영자는 다음의 경우 사전 통보 없이 서비스 이용을 제한할 수 있습니다.")
    BulletText("약관 위반 행위가 확인된 경우")
    BulletText("서비스 안정성을 저해하는 행위가 발생한 경우")
    BulletText("시스템 점검 또는 긴급 유지보수가 필요한 경우")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제6조 (개인정보 보호)")
    BodyText("서비스 운영자는 관련 법령에 따라 이용자의 개인정보를 보호하며, 개인정보 처리와 관련된 사항은 개인정보 수집 및 이용 동의 내용에 따릅니다.")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제7조 (면책사항)")
    BodyText("1. 서비스에서 제공하는 셔틀 위치 및 도착 정보는 실제 운행 상황에 따라 차이가 발생할 수 있습니다.")
    BodyText("2. 천재지변, 통신 장애, 서버 오류 등 불가항력적 사유로 발생한 서비스 장애에 대해 운영자는 책임을 지지 않습니다.")
    Spacer(Modifier.height(12.dp))

    ArticleTitle("제8조 (약관 변경)")
    BodyText("운영자는 필요한 경우 관련 법령을 위반하지 않는 범위에서 약관을 변경할 수 있으며, 변경 사항은 서비스 내 공지를 통해 안내합니다.")
    Spacer(Modifier.height(12.dp))

    HorizontalDivider(color = DividerColor)
    Spacer(Modifier.height(8.dp))
    BodyText("부칙: 본 약관은 2026년 5월 11일부터 시행합니다.")
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
}

@Composable
private fun ArticleTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BodyText(text: String) {
    Text(text, fontSize = 14.sp, color = Color(0xFF333333), lineHeight = 22.sp)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BulletText(text: String) {
    Text(
        text = "• $text",
        fontSize = 14.sp,
        color = Color(0xFF555555),
        lineHeight = 22.sp,
        modifier = Modifier.padding(start = 8.dp)
    )
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun NoteText(text: String) {
    Text(text, fontSize = 13.sp, color = Color(0xFF666666), lineHeight = 20.sp)
}
