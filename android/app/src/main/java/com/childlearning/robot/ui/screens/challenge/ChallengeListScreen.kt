package com.childlearning.robot.ui.screens.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.core.network.ChallengeCard
import com.childlearning.robot.ui.components.LevelBadge

@Composable
fun ChallengeListScreen(
    onChallengeClick: (Long) -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val challenges = viewModel.dailyChallenges.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDailyChallenges()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = "🎮 魔法挑战乐园",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when (uiState.value) {
            is ChallengeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is ChallengeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = (uiState.value as ChallengeUiState.Error).message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is ChallengeUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(challenges.value) { challenge ->
                        ChallengeItem(
                            challenge = challenge,
                            onClick = { onChallengeClick(challenge.id) }
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ChallengeItem(
    challenge: ChallengeCard,
    onClick: () -> Unit
) {
    val domainColor = getDomainColor(challenge.domainKey)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = challenge.domainIcon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.background(
                    color = domainColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ).padding(8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LevelBadge(text = challenge.domainName, color = domainColor)
                    LevelBadge(text = "难度 ${"⭐".repeat(challenge.difficulty)}", color = Color(0xFFf59e0b))
                    LevelBadge(text = "+${challenge.expReward} exp", color = Color(0xFF10b981))
                    if (challenge.completed) {
                        LevelBadge(text = "✅ 已完成", color = Color(0xFF10b981))
                    }
                }
            }
        }
    }
}

fun getDomainColor(domainKey: String): Color {
    return when (domainKey) {
        "ENGLISH" -> Color(0xFF3b82f6)
        "SCIENCE" -> Color(0xFF10b981)
        "VALUES" -> Color(0xFFec4899)
        "LOGIC" -> Color(0xFF8b5cf6)
        "ENGINEERING" -> Color(0xFFf59e0b)
        "PATRIOTISM" -> Color(0xFFef4444)
        "COMPREHENSION" -> Color(0xFF06b6d4)
        "MATH" -> Color(0xFF84cc16)
        "LIFESKILL" -> Color(0xFFf97316)
        "CREATIVITY" -> Color(0xFFa855f7)
        else -> Color(0xFF6366f1)
    }
}
