package com.ai.assistance.operit.data.converter

import com.ai.assistance.operit.data.model.ChatHistory
import com.ai.assistance.operit.data.model.ChatMessage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Markdown 格式转换器
 * 支持多种 Markdown 对话格式
 */
class MarkdownConverter : ChatFormatConverter {
    
    private val dateFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    )
    
    override fun convert(content: String): List<ChatHistory> {
        return try {
            // 尝试分割多个对话（如果有分隔符）
            val conversations = splitConversations(content)
            conversations.mapNotNull { parseConversation(it) }
        } catch (e: Exception) {
            throw ConversionException("解析 Markdown 格式失败: ${e.message}", e)
        }
    }
    
    override fun getSupportedFormat(): ChatFormat = ChatFormat.MARKDOWN
    
    /**
     * 分割多个对话（如果存在）
     */
    private fun splitConversations(content: String): List<String> {
        // 尝试通过 "---" 或 "# " 分割
        val parts = content.split(Regex("\n---+\n|\n# "))
            .filter { it.trim().isNotEmpty() }
        
        return if (parts.size > 1) {
            // 重新添加标题标记
            parts.mapIndexed { index, part ->
                if (index > 0 && !part.trim().startsWith("#")) {
                    "# $part"
                } else {
                    part
                }
            }
        } else {
            listOf(content)
        }
    }
    
    /**
     * 解析单个对话
     */
    private fun parseConversation(content: String): ChatHistory? {
        val lines = content.lines()
        val messages = mutableListOf<ChatMessage>()
        
        var title = "Imported from Markdown"
        var createdAt = LocalDateTime.now()
        var currentRole: String? = null
        var currentContent = StringBuilder()
        var metadata = mutableMapOf<String, String>()
        
        var i = 0
        
        // 解析 YAML front matter（如果存在）
        if (lines.firstOrNull()?.trim() == "---") {
            i = 1
            while (i < lines.size && lines[i].trim() != "---") {
                val line = lines[i].trim()
                if (line.contains(":")) {
                    val (key, value) = line.split(":", limit = 2)
                    metadata[key.trim()] = value.trim()
                }
                i++
            }
            i++ // 跳过结束的 ---
            
            // 提取元数据
            metadata["title"]?.let { title = it }
            metadata["created"]?.let { createdAt = parseDate(it) ?: createdAt }
        }
        
        // 基准时间戳（用于生成消息的递增时间戳）
        var baseTimestamp = System.currentTimeMillis()
        var messageIndex = 0
        
        // 解析消息内容
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            
            when {
                // H1 标题作为对话标题
                trimmed.startsWith("# ") && currentRole == null -> {
                    title = trimmed.substring(2).trim()
                }
                
                // H2 标题作为角色标记
                trimmed.startsWith("## ") -> {
                    // 保存上一条消息
                    if (currentRole != null && currentContent.isNotEmpty()) {
                        messages.add(createMessage(
                            currentRole, 
                            currentContent.toString(),
                            baseTimestamp + (messageIndex * 100L)  // 每条消息间隔 100ms
                        ))
                        messageIndex++
                        currentContent.clear()
                    }
                    
                    // 解析新角色
                    val roleText = trimmed.substring(3).trim()
                    currentRole = parseRole(roleText)
                }
                
                // 内容行
                currentRole != null -> {
                    currentContent.append(line).append("\n")
                }
                
                else -> {
                    // 跳过其他行
                }
            }
            
            i++
        }
        
        // 保存最后一条消息
        if (currentRole != null && currentContent.isNotEmpty()) {
            messages.add(createMessage(
                currentRole, 
                currentContent.toString(),
                baseTimestamp + (messageIndex * 100L)
            ))
        }
        
        if (messages.isEmpty()) {
            return null
        }
        
        return ChatHistory(
            id = UUID.randomUUID().toString(),
            title = title,
            messages = messages,
            createdAt = createdAt,
            updatedAt = LocalDateTime.now(),
            group = "从 Markdown 导入"
        )
    }
    
    /**
     * 解析角色标记
     */
    private fun parseRole(roleText: String): String {
        val lower = roleText.lowercase()
        return when {
            lower.contains("user") || lower.contains("用户") || lower.contains("👤") -> "user"
            lower.contains("assistant") || lower.contains("ai") || 
            lower.contains("助手") || lower.contains("🤖") -> "ai"
            lower.contains("system") || lower.contains("系统") -> "user" // 系统消息作为用户消息
            else -> "user" // 默认为用户
        }
    }
    
    /**
     * 创建消息对象
     */
    private fun createMessage(role: String, content: String, timestamp: Long): ChatMessage {
        return ChatMessage(
            sender = role,
            content = content.trim(),
            timestamp = timestamp,
            provider = "Imported",
            modelName = "markdown"
        )
    }
    
    /**
     * 尝试解析日期字符串
     */
    private fun parseDate(dateStr: String): LocalDateTime? {
        for (formatter in dateFormatters) {
            try {
                return LocalDateTime.parse(dateStr, formatter)
            } catch (_: DateTimeParseException) {
                continue
            }
        }
        return null
    }
}
