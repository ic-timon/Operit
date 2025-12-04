package com.ai.assistance.operit.data.exporter

import com.ai.assistance.operit.data.model.ChatHistory
import com.ai.assistance.operit.data.model.ChatMessage
import java.time.format.DateTimeFormatter

/**
 * Markdown 格式导出器
 */
object MarkdownExporter {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    /**
     * 导出单个对话为 Markdown
     */
    fun exportSingle(chatHistory: ChatHistory): String {
        val sb = StringBuilder()
        
        // YAML Front Matter
        sb.appendLine("---")
        sb.appendLine("title: ${chatHistory.title}")
        sb.appendLine("created: ${chatHistory.createdAt.format(dateFormatter)}")
        sb.appendLine("updated: ${chatHistory.updatedAt.format(dateFormatter)}")
        if (chatHistory.group != null) {
            sb.appendLine("group: ${chatHistory.group}")
        }
        sb.appendLine("messages: ${chatHistory.messages.size}")
        sb.appendLine("---")
        sb.appendLine()
        
        // 标题
        sb.appendLine("# ${chatHistory.title}")
        sb.appendLine()
        
        // 元信息
        sb.appendLine("**创建时间:** ${chatHistory.createdAt.format(dateFormatter)}")
        sb.appendLine("**更新时间:** ${chatHistory.updatedAt.format(dateFormatter)}")
        if (chatHistory.group != null) {
            sb.appendLine("**分组:** ${chatHistory.group}")
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        
        // 消息内容
        for (message in chatHistory.messages) {
            appendMessage(sb, message)
        }
        
        return sb.toString()
    }
    
    /**
     * 导出多个对话为 Markdown
     */
    fun exportMultiple(chatHistories: List<ChatHistory>): String {
        val sb = StringBuilder()
        
        sb.appendLine("# 聊天记录导出")
        sb.appendLine()
        sb.appendLine("**导出时间:** ${java.time.LocalDateTime.now().format(dateFormatter)}")
        sb.appendLine("**对话数量:** ${chatHistories.size}")
        sb.appendLine("**总消息数:** ${chatHistories.sumOf { it.messages.size }}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        
        for ((index, chatHistory) in chatHistories.withIndex()) {
            if (index > 0) {
                sb.appendLine()
                sb.appendLine("---")
                sb.appendLine()
            }
            
            sb.append(exportSingle(chatHistory))
        }
        
        return sb.toString()
    }
    
    /**
     * 添加单条消息
     */
    private fun appendMessage(sb: StringBuilder, message: ChatMessage) {
        // 角色标题
        val roleIcon = if (message.sender == "user") "👤" else "🤖"
        val roleText = if (message.sender == "user") "User" else "Assistant"
        sb.appendLine("## $roleIcon $roleText")
        sb.appendLine()
        
        // 消息元数据（可选）
        if (message.modelName.isNotEmpty() && message.modelName != "markdown" && message.modelName != "unknown") {
            sb.appendLine("*Model: ${message.modelName}*")
            sb.appendLine()
        }
        
        // 消息内容
        sb.appendLine(message.content)
        sb.appendLine()
    }
}
