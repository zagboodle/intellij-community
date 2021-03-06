package org.jetbrains.plugins.github.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UI.PanelFactory.grid
import com.intellij.util.ui.UI.PanelFactory.panel
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.authentication.ui.GithubAccountCombobox
import org.jetbrains.plugins.github.ui.util.DialogValidationUtils.RecordUniqueValidator
import org.jetbrains.plugins.github.ui.util.DialogValidationUtils.chain
import org.jetbrains.plugins.github.ui.util.DialogValidationUtils.notBlank
import org.jetbrains.plugins.github.ui.util.Validator
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JTextArea

class GithubShareDialog(project: Project,
                        accounts: Set<GithubAccount>,
                        defaultAccount: GithubAccount?,
                        existingRemotes: Set<String>,
                        private val accountsInfo: Map<GithubAccount,
                          Pair<@ParameterName("isPrivateRepoAllowed") Boolean, @ParameterName("existingRepos") Set<String>>>)
  : DialogWrapper(project) {

  private val GITHUB_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+")

  private val repositoryTextField = JBTextField(project.name)
  private val privateCheckBox = JBCheckBox("Private", false)
  private val remoteTextField = JBTextField(if (existingRemotes.isEmpty()) "origin" else "github")
  private val descriptionTextArea = JTextArea()
  private val accountSelector = GithubAccountCombobox(accounts, defaultAccount) { switchAccount(it) }
  private val existingRepoValidator = RecordUniqueValidator(repositoryTextField, "Repository with selected name already exists")
  private val existingRemoteValidator = RecordUniqueValidator(remoteTextField, "Remote with selected name already exists")
    .apply { records = existingRemotes }

  init {
    title = "Share Project On GitHub"
    setOKButtonText("Share")
    init()
    switchAccount(accountSelector.selectedItem as GithubAccount)
  }

  private fun switchAccount(account: GithubAccount) {
    accountsInfo[account]!!.let {
      privateCheckBox.isEnabled = it.first
      if (!it.first) privateCheckBox.toolTipText = "Your account doesn't support private repositories"
      existingRepoValidator.records = it.second
    }
  }

  override fun createCenterPanel(): JComponent? {
    val descriptionPane = JBScrollPane(descriptionTextArea).apply {
      minimumSize = JBDimension(150, 50)
      preferredSize = JBDimension(150, 50)
    }

    val repository = JBBox.createHorizontalBox()
    repository.add(repositoryTextField)
    repository.add(JBBox.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)))
    repository.add(privateCheckBox)
    repository.add(JBBox.createRigidArea(JBUI.size(5, 0)))

    return grid().resize()
      .add(panel(repository).withLabel("Repository name:"))
      .add(panel(remoteTextField).withLabel("Remote:"))
      .add(panel(descriptionPane).withLabel("Description:").anchorLabelOn(UI.Anchor.Top).resizeY(true))
      .apply {
        if (accountSelector.isEnabled) add(panel(accountSelector).withLabel("Share by:").resizeX(false))
      }
      .createPanel()
  }

  override fun doValidateAll(): List<ValidationInfo> {
    val repositoryNamePatternMatchValidator: Validator = {
      if (!GITHUB_REPO_PATTERN.matcher(repositoryTextField.text).matches()) ValidationInfo(
        "Invalid repository name. Name should consist of letters, numbers, dashes, dots and underscores",
        repositoryTextField)
      else null
    }

    return listOf(
      chain({ notBlank(repositoryTextField, "No repository name selected") },
            repositoryNamePatternMatchValidator,
            existingRepoValidator),
      chain({ notBlank(remoteTextField, "No remote name selected") },
            existingRemoteValidator)
    ).mapNotNull { it() }
  }

  override fun getHelpId() = "github.share"
  override fun getDimensionServiceKey() = "Github.ShareDialog"
  override fun getPreferredFocusedComponent() = repositoryTextField

  fun getRepositoryName(): String = repositoryTextField.text
  fun getRemoteName(): String = remoteTextField.text
  fun isPrivate(): Boolean = privateCheckBox.isSelected
  fun getDescription(): String = descriptionTextArea.text
  fun getAccount(): GithubAccount = accountSelector.selectedItem as GithubAccount

  @TestOnly
  fun testSetRepositoryName(name: String) {
    repositoryTextField.text = name
  }
}
