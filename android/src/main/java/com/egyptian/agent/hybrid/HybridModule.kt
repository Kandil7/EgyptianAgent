package com.egyptian.agent.hybrid

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.egyptian.agent.accessibility.ui.AccessibilityTreeParser
import com.egyptian.agent.ai.FunctionGemmaIntentEngine
import com.egyptian.agent.navigation.UINavigationEngine
import com.egyptian.agent.workflow.WorkflowEngine

/**
 * Dependency Injection Module for Hybrid Architecture.
 *
 * Provides AccessibilityService and initializes all hybrid components
 * with proper singleton patterns.
 *
 * Features:
 * - Centralized component initialization
 * - Singleton pattern for engines
 * - Proper lifecycle management
 * - AccessibilityService injection
 *
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
object HybridModule {

    @Volatile
    private var instance: HybridModule? = null

    private var context: Context? = null
    private var accessibilityService: AccessibilityService? = null

    // Core components (lazy initialization)
    private var _functionGemmaEngine: FunctionGemmaIntentEngine? = null
    private var _uiNavigationEngine: UINavigationEngine? = null
    private var _workflowEngine: WorkflowEngine? = null
    private var _hybridOrchestrator: HybridOrchestrator? = null
    private var _treeParser: AccessibilityTreeParser? = null

    /**
     * Initialize the Hybrid Module.
     * Must be called before using any hybrid components.
     *
     * @param appContext Application context
     * @param service AccessibilityService instance
     */
    fun initialize(appContext: Context, service: AccessibilityService) {
        context = appContext.applicationContext
        accessibilityService = service

        // Initialize components
        functionGemmaEngine
        uiNavigationEngine
        workflowEngine
        hybridOrchestrator
        treeParser

        instance = this
    }

    /**
     * Get the singleton instance.
     */
    fun getInstance(): HybridModule {
        return instance ?: throw IllegalStateException(
            "HybridModule not initialized. Call initialize() first."
        )
    }

    /**
     * Get AccessibilityService.
     */
    fun getAccessibilityService(): AccessibilityService {
        return accessibilityService ?: throw IllegalStateException(
            "AccessibilityService not available"
        )
    }

    /**
     * Get Context.
     */
    fun getContext(): Context {
        return context ?: throw IllegalStateException(
            "Context not available"
        )
    }

    /**
     * Get FunctionGemma Intent Engine (singleton).
     */
    val functionGemmaEngine: FunctionGemmaIntentEngine
        get() {
            if (_functionGemmaEngine == null) {
                synchronized(this) {
                    if (_functionGemmaEngine == null) {
                        _functionGemmaEngine = FunctionGemmaIntentEngine(getContext())
                    }
                }
            }
            return _functionGemmaEngine!!
        }

    /**
     * Get UI Navigation Engine (singleton).
     */
    val uiNavigationEngine: UINavigationEngine
        get() {
            if (_uiNavigationEngine == null) {
                synchronized(this) {
                    if (_uiNavigationEngine == null) {
                        _uiNavigationEngine = UINavigationEngine(
                            getAccessibilityService(),
                            getContext()
                        )
                    }
                }
            }
            return _uiNavigationEngine!!
        }

    /**
     * Get Workflow Engine (singleton).
     */
    val workflowEngine: WorkflowEngine
        get() {
            if (_workflowEngine == null) {
                synchronized(this) {
                    if (_workflowEngine == null) {
                        _workflowEngine = WorkflowEngine(
                            getContext(),
                            uiNavigationEngine
                        )
                    }
                }
            }
            return _workflowEngine!!
        }

    /**
     * Get Hybrid Orchestrator (singleton).
     */
    val hybridOrchestrator: HybridOrchestrator
        get() {
            if (_hybridOrchestrator == null) {
                synchronized(this) {
                    if (_hybridOrchestrator == null) {
                        _hybridOrchestrator = HybridOrchestrator.create(getContext())
                    }
                }
            }
            return _hybridOrchestrator!!
        }

    /**
     * Get Accessibility Tree Parser.
     */
    val treeParser: AccessibilityTreeParser
        get() {
            if (_treeParser == null) {
                synchronized(this) {
                    if (_treeParser == null) {
                        _treeParser = AccessibilityTreeParser(getAccessibilityService())
                    }
                }
            }
            return _treeParser!!
        }

    /**
     * Check if module is initialized.
     */
    fun isInitialized(): Boolean {
        return instance != null && context != null && accessibilityService != null
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        _hybridOrchestrator?.destroy()
        _functionGemmaEngine = null
        _uiNavigationEngine = null
        _workflowEngine = null
        _hybridOrchestrator = null
        _treeParser = null
        accessibilityService = null
        context = null
        instance = null
    }

    /**
     * Check if all components are ready.
     */
    fun isReady(): Boolean {
        return isInitialized() &&
                functionGemmaEngine.isReady() &&
                uiNavigationEngine.isReady() &&
                hybridOrchestrator.isReady()
    }
}
