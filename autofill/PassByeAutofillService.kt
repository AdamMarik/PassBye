package com.project.passbye.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.View
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.project.passbye.R
import com.project.passbye.util.EncryptedStorage

class PassByeAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val allNodes = mutableListOf<AssistStructure.ViewNode>()

        for (i in 0 until structure.windowNodeCount) {
            traverseViewNodes(structure.getWindowNodeAt(i).rootViewNode, allNodes)
        }

        val usernameNode = allNodes.find {
            it.autofillHints?.contains(View.AUTOFILL_HINT_USERNAME) == true ||
                    it.hint?.contains("email", ignoreCase = true) == true ||
                    it.idEntry?.contains("user", ignoreCase = true) == true
        }

        val passwordNode = allNodes.find {
            it.autofillHints?.contains(View.AUTOFILL_HINT_PASSWORD) == true ||
                    it.hint?.contains("password", ignoreCase = true) == true ||
                    it.idEntry?.contains("pass", ignoreCase = true) == true
        }

        if (usernameNode != null && passwordNode != null) {
            val usernameId = usernameNode.autofillId ?: return callback.onSuccess(null)
            val passwordId = passwordNode.autofillId ?: return callback.onSuccess(null)

            val currentUser = getLoggedInUsername() ?: return callback.onSuccess(null)
            val storage = EncryptedStorage(this)
            val credentials = storage.loadPasswordsForUser(currentUser)

            val domain = extractDomainFromStructure(structure)

            val responseBuilder = FillResponse.Builder()

            credentials.forEach { (fullKey, savedPassword) ->
                val parts = fullKey.split("|")
                if (parts.size != 3) return@forEach

                val usernamePart = parts[0]
                val domainPart = parts[1]
                val siteUsername = parts[2]

                if (!domainPart.contains(domain, ignoreCase = true)) return@forEach

                val presentation = RemoteViews(packageName, R.layout.autofill_item).apply {
                    setTextViewText(R.id.username, siteUsername)
                    setTextViewText(R.id.domain, domainPart)
                }

                val dataset = Dataset.Builder(presentation)
                    .setValue(usernameId, AutofillValue.forText(siteUsername), presentation)
                    .setValue(passwordId, AutofillValue.forText(savedPassword), presentation)
                    .build()

                responseBuilder.addDataset(dataset)
            }

            callback.onSuccess(responseBuilder.build())
        } else {
            callback.onSuccess(null)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun traverseViewNodes(node: AssistStructure.ViewNode, list: MutableList<AssistStructure.ViewNode>) {
        list.add(node)
        for (i in 0 until node.childCount) {
            traverseViewNodes(node.getChildAt(i), list)
        }
    }

    private fun extractDomainFromStructure(structure: AssistStructure): String {
        for (i in 0 until structure.windowNodeCount) {
            val rootNode = structure.getWindowNodeAt(i).rootViewNode
            val domain = rootNode.webDomain ?: rootNode.idEntry ?: ""
            if (domain.isNotBlank()) {
                return domain.removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .split("/")[0]
                    .lowercase()
            }
        }
        return ""
    }

    private fun getLoggedInUsername(): String? {
        val prefs = getSharedPreferences("PassByePrefs", MODE_PRIVATE)
        return prefs.getString("currentUser", null)
    }
}
