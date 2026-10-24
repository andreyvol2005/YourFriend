package com.example.yourfriend

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yourfriend.chat.ChatAdapter
import com.example.yourfriend.chat.ChatItemAnimator
import com.example.yourfriend.chat.FriendPersona
import com.example.yourfriend.chat.GigaChatClient
import com.example.yourfriend.chat.GigaChatException
import com.example.yourfriend.chat.cleanReply
import com.example.yourfriend.data.ChatMessage
import com.example.yourfriend.data.ChatStore
import com.example.yourfriend.data.FriendGender
import com.example.yourfriend.data.FriendProfile
import com.example.yourfriend.ui.AvatarOrbView
import com.example.yourfriend.ui.MagicBackgroundView
import com.example.yourfriend.ui.SparkleBurstView
import com.example.yourfriend.ui.dp
import com.example.yourfriend.ui.installPressFeedback
import com.example.yourfriend.ui.shake
import com.example.yourfriend.ui.softHaptic
import com.example.yourfriend.ui.springScale
import com.example.yourfriend.ui.staggerIn
import com.example.yourfriend.ui.tapHaptic
import java.util.Locale
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var store: ChatStore
    private val adapter = ChatAdapter()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var root: FrameLayout
    private lateinit var magicBackground: MagicBackgroundView
    private lateinit var sparkleBurst: SparkleBurstView
    private lateinit var onboardingRoot: FrameLayout
    private lateinit var chatRoot: FrameLayout
    private lateinit var chatContent: ViewGroup

    private lateinit var onboardingScroll: NestedScrollView
    private lateinit var onboardingAvatar: AvatarOrbView
    private lateinit var onboardingEyebrow: TextView
    private lateinit var onboardingTitle: TextView
    private lateinit var onboardingSubtitle: TextView
    private lateinit var onboardingQuote: TextView
    private lateinit var stepIndicator: LinearLayout
    private lateinit var stepDotOne: View
    private lateinit var stepDotTwo: View
    private lateinit var stepGender: LinearLayout
    private lateinit var cardFemale: LinearLayout
    private lateinit var cardMale: LinearLayout
    private lateinit var iconFemale: TextView
    private lateinit var iconMale: TextView
    private lateinit var titleFemale: TextView
    private lateinit var titleMale: TextView
    private lateinit var hintFemale: TextView
    private lateinit var hintMale: TextView
    private lateinit var stepName: LinearLayout
    private lateinit var nameLabel: TextView
    private lateinit var nameInput: EditText
    private lateinit var suggestNameOne: TextView
    private lateinit var suggestNameTwo: TextView
    private lateinit var suggestNameThree: TextView
    private lateinit var onboardingBack: ImageButton
    private lateinit var ctaButton: AppCompatButton
    private lateinit var ctaHint: TextView

    private lateinit var chatHeader: LinearLayout
    private lateinit var chatAvatar: AvatarOrbView
    private lateinit var chatName: TextView
    private lateinit var chatStatus: TextView
    private lateinit var onlineDot: View
    private lateinit var chatMore: ImageButton
    private lateinit var chatRecycler: RecyclerView
    private lateinit var suggestionsScroll: View
    private lateinit var suggestionOne: TextView
    private lateinit var suggestionTwo: TextView
    private lateinit var suggestionThree: TextView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton

    private var profile: FriendProfile? = null
    private lateinit var gigaChat: GigaChatClient
    private var selectedGender: FriendGender? = null
    private var step = 1
    private var chatVisible = false
    private var waitingForReply = false
    private var chatGeneration = 0
    private var onlinePulse: ValueAnimator? = null
    private var idCounter = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_main)
        store = ChatStore(this)
        gigaChat = GigaChatClient(this)
        bindViews()
        applyWindowInsets()
        setupOnboarding()
        setupChat()
        setupBackNavigation()

        val savedProfile = store.loadProfile()
        if (savedProfile == null) {
            showOnboarding(animate = true)
        } else {
            profile = savedProfile
            showChat(animate = false)
        }
    }

    private fun bindViews() {
        root = findViewById(R.id.root)
        magicBackground = findViewById(R.id.magicBackground)
        sparkleBurst = findViewById(R.id.sparkleBurst)
        onboardingRoot = findViewById(R.id.onboardingRoot)
        chatRoot = findViewById(R.id.chatRoot)
        chatContent = findViewById(R.id.chatContent)

        onboardingScroll = findViewById(R.id.onboardingScroll)
        onboardingAvatar = findViewById(R.id.onboardingAvatar)
        onboardingEyebrow = findViewById(R.id.onboardingEyebrow)
        onboardingTitle = findViewById(R.id.onboardingTitle)
        onboardingSubtitle = findViewById(R.id.onboardingSubtitle)
        onboardingQuote = findViewById(R.id.onboardingQuote)
        stepIndicator = findViewById(R.id.stepIndicator)
        stepDotOne = findViewById(R.id.stepDotOne)
        stepDotTwo = findViewById(R.id.stepDotTwo)
        stepGender = findViewById(R.id.stepGender)
        cardFemale = findViewById(R.id.cardFemale)
        cardMale = findViewById(R.id.cardMale)
        iconFemale = findViewById(R.id.iconFemale)
        iconMale = findViewById(R.id.iconMale)
        titleFemale = findViewById(R.id.titleFemale)
        titleMale = findViewById(R.id.titleMale)
        hintFemale = findViewById(R.id.hintFemale)
        hintMale = findViewById(R.id.hintMale)
        stepName = findViewById(R.id.stepName)
        nameLabel = findViewById(R.id.nameLabel)
        nameInput = findViewById(R.id.nameInput)
        suggestNameOne = findViewById(R.id.suggestNameOne)
        suggestNameTwo = findViewById(R.id.suggestNameTwo)
        suggestNameThree = findViewById(R.id.suggestNameThree)
        onboardingBack = findViewById(R.id.onboardingBack)
        ctaButton = findViewById(R.id.ctaButton)
        ctaHint = findViewById(R.id.ctaHint)

        chatHeader = findViewById(R.id.chatHeader)
        chatAvatar = findViewById(R.id.chatAvatar)
        chatName = findViewById(R.id.chatName)
        chatStatus = findViewById(R.id.chatStatus)
        onlineDot = findViewById(R.id.onlineDot)
        chatMore = findViewById(R.id.chatMore)
        chatRecycler = findViewById(R.id.chatRecycler)
        suggestionsScroll = findViewById(R.id.suggestionsScroll)
        suggestionOne = findViewById(R.id.suggestionOne)
        suggestionTwo = findViewById(R.id.suggestionTwo)
        suggestionThree = findViewById(R.id.suggestionThree)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottom = max(bars.bottom, ime)
            onboardingRoot.setPadding(0, bars.top, 0, bottom)
            chatRoot.setPadding(0, bars.top, 0, bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun setupOnboarding() {
        magicBackground.setGender(null)
        onboardingAvatar.setProfile("?", null)
        cardFemale.installPressFeedback(0.97f)
        cardMale.installPressFeedback(0.97f)
        ctaButton.installPressFeedback(0.985f)
        onboardingBack.installPressFeedback(0.9f)
        suggestNameOne.installPressFeedback(0.95f)
        suggestNameTwo.installPressFeedback(0.95f)
        suggestNameThree.installPressFeedback(0.95f)

        cardFemale.setOnClickListener { selectGender(FriendGender.FEMALE) }
        cardMale.setOnClickListener { selectGender(FriendGender.MALE) }
        ctaButton.setOnClickListener {
            if (step == 1) {
                if (selectedGender != null) goToStepTwo()
            } else {
                createFriend()
            }
        }
        onboardingBack.setOnClickListener { goToStepOne() }

        suggestNameOne.setOnClickListener { fillName(suggestNameOne.text.toString()) }
        suggestNameTwo.setOnClickListener { fillName(suggestNameTwo.text.toString()) }
        suggestNameThree.setOnClickListener { fillName(suggestNameThree.text.toString()) }

        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (step == 2) {
                    setCtaEnabled(!s.isNullOrBlank())
                }
            }
        })
        nameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createFriend()
                true
            } else {
                false
            }
        }
    }

    private fun setupChat() {
        chatRecycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = false }
        chatRecycler.adapter = adapter
        chatRecycler.itemAnimator = ChatItemAnimator()
        chatRecycler.setHasFixedSize(false)

        sendButton.installPressFeedback(0.9f)
        chatMore.installPressFeedback(0.9f)
        suggestionOne.installPressFeedback(0.95f)
        suggestionTwo.installPressFeedback(0.95f)
        suggestionThree.installPressFeedback(0.95f)

        sendButton.setOnClickListener { sendCurrentMessage() }
        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateSendState()
        })

        suggestionOne.setOnClickListener { sendSuggestion(suggestionOne.text.toString()) }
        suggestionTwo.setOnClickListener { sendSuggestion(suggestionTwo.text.toString()) }
        suggestionThree.setOnClickListener { sendSuggestion(suggestionThree.text.toString()) }
        chatMore.setOnClickListener { showChatMenu() }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!chatVisible && step == 2) {
                    goToStepOne()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showOnboarding(animate: Boolean) {
        handler.removeCallbacksAndMessages(null)
        chatGeneration += 1
        gigaChat.cancelPending()
        waitingForReply = false
        chatVisible = false
        adapter.clear()
        stopOnlinePulse()
        hideKeyboard()

        chatRoot.visibility = View.GONE
        chatRoot.alpha = 1f
        chatRoot.scaleX = 1f
        chatRoot.scaleY = 1f
        chatRoot.translationY = 0f
        onboardingRoot.visibility = View.VISIBLE
        onboardingRoot.alpha = 1f
        onboardingRoot.scaleX = 1f
        onboardingRoot.scaleY = 1f
        onboardingRoot.translationY = 0f
        magicBackground.setGender(null)
        onboardingAvatar.setProfile("?", null)
        resetOnboardingState()

        if (animate) {
            onboardingRoot.alpha = 0f
            onboardingRoot.scaleX = 0.965f
            onboardingRoot.scaleY = 0.965f
            onboardingRoot.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(520L)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()
            animateOnboardingEntry()
        }
    }

    private fun resetOnboardingState() {
        step = 1
        selectedGender = null
        updateGenderCards()
        setCtaEnabled(false)

        stepGender.visibility = View.VISIBLE
        stepGender.alpha = 1f
        stepGender.translationX = 0f
        stepName.visibility = View.GONE
        stepName.alpha = 1f
        stepName.translationX = 0f
        onboardingBack.visibility = View.GONE
        suggestionOne.text = getString(R.string.suggestion_one)
        suggestionTwo.text = getString(R.string.suggestion_two)
        suggestionThree.text = getString(R.string.suggestion_three)

        onboardingTitle.text = getString(R.string.onboarding_title)
        onboardingSubtitle.text = getString(R.string.onboarding_subtitle)
        ctaButton.text = getString(R.string.onboarding_next)
        ctaHint.text = getString(R.string.onboarding_edit_later)
        nameInput.setText("")
        nameInput.clearFocus()

        setStepDots(second = false, animate = false)
        onboardingScroll.post { onboardingScroll.scrollTo(0, 0) }
    }

    private fun animateOnboardingEntry() {
        onboardingAvatar.alpha = 0f
        onboardingAvatar.scaleX = 0.78f
        onboardingAvatar.scaleY = 0.78f
        onboardingAvatar.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(820L)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        onboardingEyebrow.staggerIn(100L, 12f)
        onboardingTitle.staggerIn(175L, 22f)
        onboardingSubtitle.staggerIn(255L, 16f)
        stepIndicator.staggerIn(330L, 10f)
        stepGender.staggerIn(395L, 18f)
        onboardingQuote.staggerIn(450L, 9f)
        ctaButton.translationY = dp(12f).toFloat()
        ctaButton.animate()
            .translationY(0f)
            .setStartDelay(490L)
            .setDuration(560L)
            .setInterpolator(DecelerateInterpolator(1.8f))
            .start()
        ctaHint.staggerIn(540L, 8f)
    }

    private fun selectGender(gender: FriendGender) {
        selectedGender = gender
        magicBackground.setGender(gender)
        onboardingAvatar.setProfile("?", gender)
        updateGenderCards()
        setCtaEnabled(true)
        root.tapHaptic()

        val selectedCard = if (gender == FriendGender.FEMALE) cardFemale else cardMale
        selectedCard.springScale(
            target = 1.025f,
            stiffness = SpringForce.STIFFNESS_MEDIUM,
            damping = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        )
        handler.postDelayed({
            selectedCard.springScale(1f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_NO_BOUNCY)
        }, 260L)
    }

    private fun updateGenderCards() {
        val femaleSelected = selectedGender == FriendGender.FEMALE
        val maleSelected = selectedGender == FriendGender.MALE

        cardFemale.background = genderCardBackground(femaleSelected, FriendGender.FEMALE)
        cardMale.background = genderCardBackground(maleSelected, FriendGender.MALE)
        cardFemale.elevation = if (femaleSelected) dp(8f).toFloat() else 0f
        cardMale.elevation = if (maleSelected) dp(8f).toFloat() else 0f

        val primary = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_muted)
        val selectedHint = 0xE6FFFFFF.toInt()

        titleFemale.setTextColor(primary)
        titleMale.setTextColor(primary)
        hintFemale.setTextColor(if (femaleSelected) selectedHint else muted)
        hintMale.setTextColor(if (maleSelected) selectedHint else muted)
        iconFemale.alpha = if (femaleSelected) 1f else 0.72f
        iconMale.alpha = if (maleSelected) 1f else 0.72f
    }

    private fun genderCardBackground(selected: Boolean, gender: FriendGender): GradientDrawable {
        val colors = if (selected) {
            if (gender == FriendGender.FEMALE) {
                intArrayOf(0x3DFF7AB6, 0x1A9B6CFF)
            } else {
                intArrayOf(0x3D5CE1E6, 0x1A5B8CFF)
            }
        } else {
            intArrayOf(0x1FFFFFFF, 0x08FFFFFF)
        }
        val stroke = if (selected) {
            if (gender == FriendGender.FEMALE) 0x99FF7AB6.toInt() else 0x995CE1E6.toInt()
        } else {
            0x2BFFFFFF
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(26f).toFloat()
            setStroke(dp(1.1f), stroke)
        }
    }

    private fun goToStepTwo() {
        if (step == 2) return
        step = 2
        root.tapHaptic()
        val female = selectedGender == FriendGender.FEMALE

        nameLabel.text = getString(
            if (female) R.string.onboarding_step_two_female else R.string.onboarding_step_two
        )
        nameInput.hint = if (female) "Например, Ая" else "Например, Лео"
        suggestNameOne.text = if (female) "Ая" else "Лео"
        suggestNameTwo.text = if (female) "Мия" else "Марк"
        suggestNameThree.text = if (female) "Ева" else "Ной"
        ctaButton.text = getString(R.string.onboarding_create)
        ctaHint.text = "Имя будет видно только тебе"

        crossfadeText(
            onboardingTitle,
            if (female) "Как её зовут?" else "Как его зовут?"
        )
        crossfadeText(
            onboardingSubtitle,
            "Имя — первое, что делает друга по-настоящему близким."
        )

        stepGender.animate()
            .alpha(0f)
            .translationX(-dp(24f).toFloat())
            .setDuration(210L)
            .withEndAction {
                stepGender.visibility = View.GONE
                stepGender.alpha = 1f
                stepGender.translationX = 0f
                stepName.visibility = View.VISIBLE
                stepName.alpha = 0f
                stepName.translationX = dp(26f).toFloat()
                stepName.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(360L)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .start()
            }
            .start()

        onboardingBack.visibility = View.VISIBLE
        onboardingBack.alpha = 0f
        onboardingBack.animate().alpha(1f).setDuration(260L).start()
        setStepDots(second = true, animate = true)
        setCtaEnabled(nameInput.text.isNotBlank())

        onboardingScroll.postDelayed({
            if (step == 2) {
                onboardingScroll.smoothScrollTo(0, stepName.top - dp(8f))
                nameInput.requestFocus()
                showKeyboard(nameInput)
            }
        }, 380L)
    }

    private fun goToStepOne() {
        if (step == 1) return
        step = 1
        hideKeyboard()
        root.tapHaptic()
        onboardingBack.animate()
            .alpha(0f)
            .setDuration(180L)
            .withEndAction { onboardingBack.visibility = View.GONE }
            .start()

        crossfadeText(onboardingTitle, getString(R.string.onboarding_title))
        crossfadeText(onboardingSubtitle, getString(R.string.onboarding_subtitle))

        stepName.animate()
            .alpha(0f)
            .translationX(dp(22f).toFloat())
            .setDuration(190L)
            .withEndAction {
                stepName.visibility = View.GONE
                stepName.alpha = 1f
                stepName.translationX = 0f
                stepGender.visibility = View.VISIBLE
                stepGender.alpha = 0f
                stepGender.translationX = -dp(24f).toFloat()
                stepGender.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(330L)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .start()
            }
            .start()

        ctaButton.text = getString(R.string.onboarding_next)
        ctaHint.text = getString(R.string.onboarding_edit_later)
        setCtaEnabled(selectedGender != null)
        setStepDots(second = false, animate = true)
        onboardingScroll.postDelayed({ onboardingScroll.smoothScrollTo(0, 0) }, 220L)
    }

    private fun crossfadeText(view: TextView, newText: String) {
        view.animate()
            .alpha(0f)
            .translationY(-dp(7f).toFloat())
            .setDuration(150L)
            .withEndAction {
                view.text = newText
                view.translationY = dp(9f).toFloat()
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300L)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .start()
            }
            .start()
    }

    private fun fillName(name: String) {
        nameInput.setText(name)
        nameInput.setSelection(name.length)
        nameInput.requestFocus()
        root.tapHaptic()
    }

    private fun setCtaEnabled(enabled: Boolean) {
        ctaButton.isEnabled = enabled
        ctaButton.animate()
            .alpha(if (enabled) 1f else 0.42f)
            .scaleX(if (enabled) 1f else 0.975f)
            .scaleY(if (enabled) 1f else 0.975f)
            .setDuration(220L)
            .start()
    }

    private fun setStepDots(second: Boolean, animate: Boolean) {
        val firstWidth = if (second) dp(8f) else dp(26f)
        val secondWidth = if (second) dp(26f) else dp(8f)
        stepDotOne.background = ContextCompat.getDrawable(
            this,
            if (second) R.drawable.bg_step_inactive else R.drawable.bg_step_active
        )
        stepDotTwo.background = ContextCompat.getDrawable(
            this,
            if (second) R.drawable.bg_step_active else R.drawable.bg_step_inactive
        )

        if (!animate) {
            setViewWidth(stepDotOne, firstWidth)
            setViewWidth(stepDotTwo, secondWidth)
            return
        }

        val fromOne = stepDotOne.width.coerceAtLeast(dp(8f))
        val fromTwo = stepDotTwo.width.coerceAtLeast(dp(8f))
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                setViewWidth(stepDotOne, (fromOne + (firstWidth - fromOne) * fraction).toInt())
                setViewWidth(stepDotTwo, (fromTwo + (secondWidth - fromTwo) * fraction).toInt())
            }
            start()
        }
    }

    private fun setViewWidth(view: View, width: Int) {
        val params = view.layoutParams
        params.width = width
        view.layoutParams = params
    }

    private fun createFriend() {
        val rawName = nameInput.text.toString().trim().replace(Regex("\\s+"), " ")
        if (rawName.isBlank()) {
            nameInput.shake()
            nameInput.requestFocus()
            return
        }
        val gender = selectedGender ?: return
        val normalizedName = rawName.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
        val newProfile = FriendProfile(normalizedName, gender)
        profile = newProfile
        store.saveProfile(newProfile)
        store.saveMessages(emptyList())
        hideKeyboard()
        root.tapHaptic()
        burstAt(onboardingAvatar, accentColor())
        transitionToChat()
    }

    private fun transitionToChat() {
        prepareChat()
        chatVisible = true
        chatRoot.visibility = View.VISIBLE
        chatRoot.alpha = 0f
        chatRoot.scaleX = 1.04f
        chatRoot.scaleY = 1.04f
        chatRoot.translationY = dp(24f).toFloat()

        onboardingRoot.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .translationY(-dp(22f).toFloat())
            .setDuration(430L)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .withEndAction {
                onboardingRoot.visibility = View.GONE
                onboardingRoot.alpha = 1f
                onboardingRoot.scaleX = 1f
                onboardingRoot.scaleY = 1f
                onboardingRoot.translationY = 0f
            }
            .start()

        chatRoot.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setStartDelay(130L)
            .setDuration(620L)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        handler.postDelayed({
            if (chatVisible) burstAt(chatAvatar, accentColor())
        }, 360L)
    }

    private fun showChat(animate: Boolean) {
        profile ?: return
        chatVisible = true
        onboardingRoot.visibility = View.GONE
        chatRoot.visibility = View.VISIBLE
        chatRoot.alpha = 1f
        chatRoot.scaleX = 1f
        chatRoot.scaleY = 1f
        chatRoot.translationY = 0f
        prepareChat()
        if (animate) {
            chatRoot.alpha = 0f
            chatRoot.scaleX = 0.98f
            chatRoot.scaleY = 0.98f
            chatRoot.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(520L)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }
    }

    private fun prepareChat() {
        val current = profile ?: return
        chatGeneration += 1
        gigaChat.cancelPending()
        adapter.setProfile(current)
        updateChatHeader()
        setChatStatus(typing = false)
        waitingForReply = false

        val loaded = store.loadMessages()
        if (loaded.isEmpty()) {
            val greeting = ChatMessage(
                id = nextId(),
                text = FriendPersona.greeting(current),
                fromUser = false
            )
            adapter.setMessages(listOf(greeting))
            store.saveMessages(adapter.snapshot())
        } else {
            adapter.setMessages(loaded)
        }

        updateSuggestionsVisibility()
        updateSendState()
        scrollToBottom(smooth = false)
    }

    private fun updateChatHeader() {
        val current = profile ?: return
        chatName.text = current.name
        chatAvatar.setProfile(current.name, current.gender)
        onlineDot.visibility = View.VISIBLE
        chatStatus.text = getString(R.string.chat_online)
    }

    private fun sendSuggestion(text: String) {
        if (waitingForReply) return
        messageInput.setText(text)
        messageInput.setSelection(text.length)
        sendCurrentMessage()
    }

    private fun sendCurrentMessage() {
        if (waitingForReply || !chatVisible) return
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return
        val current = profile ?: return

        messageInput.setText("")
        hideSuggestions()
        root.tapHaptic()
        burstAt(sendButton, accentColor())

        val message = ChatMessage(
            id = nextId(),
            text = text,
            fromUser = true
        )
        adapter.addMessage(message)
        saveChat()
        scrollToBottom(smooth = true)
        waitingForReply = true
        updateSendState()
        setChatStatus(typing = true)
        adapter.setTyping(true)
        scrollToBottom(smooth = true)

        if (!gigaChat.isConfigured()) {
            finishWithError(
                chatGeneration,
                "GigaChat не настроен: добавь ключ в gigachat.properties и пересобери приложение."
            )
            return
        }

        val generation = chatGeneration
        gigaChat.reply(current, adapter.snapshot()) { result ->
            if (generation != chatGeneration || !chatVisible) return@reply
            result.onSuccess { rawReply ->
                adapter.setTyping(false)
                setChatStatus(typing = false)
                waitingForReply = false

                val reply = ChatMessage(
                    id = nextId(),
                    text = cleanReply(rawReply, current.name),
                    fromUser = false
                )
                adapter.addMessage(reply)
                saveChat()
                updateSendState()
                scrollToBottom(smooth = true)
                celebrateReply()
            }.onFailure { error ->
                Log.w("GigaChat", "Ответ не получен: ${error.message}")
                finishWithError(generation, friendlyError(error))
            }
        }
    }

    private fun finishWithError(generation: Int, message: String) {
        if (generation != chatGeneration || !chatVisible) return
        adapter.setTyping(false)
        setChatStatus(typing = false)
        waitingForReply = false
        updateSendState()
        scrollToBottom(smooth = true)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun friendlyError(error: Throwable): String {
        return when (error) {
            is GigaChatException -> error.message ?: "Не получилось получить ответ."
            else -> "Не получилось получить ответ. Попробуй ещё раз."
        }
    }

    private fun celebrateReply() {
        handler.postDelayed({
            if (chatVisible) {
                burstAt(chatAvatar, accentColor())
                chatAvatar.springScale(
                    1.08f,
                    SpringForce.STIFFNESS_MEDIUM,
                    SpringForce.DAMPING_RATIO_LOW_BOUNCY
                )
                handler.postDelayed({
                    chatAvatar.springScale(
                        1f,
                        SpringForce.STIFFNESS_MEDIUM,
                        SpringForce.DAMPING_RATIO_NO_BOUNCY
                    )
                }, 220L)
            }
        }, 150L)
        chatStatus.softHaptic()
    }

    private fun setChatStatus(typing: Boolean) {
        chatStatus.text = getString(if (typing) R.string.chat_typing else R.string.chat_online)
        if (typing) {
            startOnlinePulse()
        } else {
            stopOnlinePulse()
        }
    }

    private fun startOnlinePulse() {
        onlinePulse?.cancel()
        onlinePulse = ValueAnimator.ofFloat(1f, 0.3f, 1f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { onlineDot.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun stopOnlinePulse() {
        onlinePulse?.cancel()
        onlinePulse = null
        onlineDot.alpha = 1f
    }

    private fun updateSendState() {
        val enabled = messageInput.text.isNotBlank() && !waitingForReply
        sendButton.isEnabled = enabled
        sendButton.animate()
            .alpha(if (enabled) 1f else 0.42f)
            .setDuration(180L)
            .start()
    }

    private fun updateSuggestionsVisibility() {
        val hasUserMessage = adapter.snapshot().any { it.fromUser }
        val target = if (hasUserMessage) View.GONE else View.VISIBLE
        if (suggestionsScroll.visibility == target) return
        TransitionManager.beginDelayedTransition(
            chatContent,
            Fade().apply { duration = 260L }
        )
        suggestionsScroll.visibility = target
    }

    private fun hideSuggestions() {
        if (suggestionsScroll.visibility != View.VISIBLE) return
        TransitionManager.beginDelayedTransition(
            chatContent,
            Fade().apply { duration = 240L }
        )
        suggestionsScroll.visibility = View.GONE
    }

    private fun scrollToBottom(smooth: Boolean) {
        chatRecycler.post {
            val count = adapter.itemCount
            if (count > 0) {
                if (smooth) {
                    chatRecycler.smoothScrollToPosition(count - 1)
                } else {
                    chatRecycler.scrollToPosition(count - 1)
                }
            }
        }
    }

    private fun saveChat() {
        if (profile != null) {
            store.saveMessages(adapter.snapshot())
        }
    }

    private fun showChatMenu() {
        PopupMenu(this, chatMore).apply {
            menuInflater.inflate(R.menu.chat_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_reset -> {
                        showResetDialog()
                        true
                    }
                    R.id.action_about -> {
                        showAboutDialog()
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun showResetDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.chat_reset_title)
            .setMessage(R.string.chat_reset_message)
            .setPositiveButton(R.string.chat_reset_confirm) { _, _ -> resetApp() }
            .setNegativeButton(R.string.chat_cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(0xFFFF7AB6.toInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(0xFFB9B1D9.toInt())
        }
        dialog.show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.chat_about_title)
            .setMessage(R.string.chat_about_message)
            .setPositiveButton(R.string.chat_about_close, null)
            .show()
    }

    private fun resetApp() {
        handler.removeCallbacksAndMessages(null)
        chatGeneration += 1
        gigaChat.cancelPending()
        waitingForReply = false
        stopOnlinePulse()
        saveChat()
        store.clear()
        profile = null
        selectedGender = null
        adapter.clear()
        hideKeyboard()
        chatRoot.visibility = View.GONE
        showOnboarding(animate = true)
    }

    private fun burstAt(view: View, color: Int) {
        val viewLocation = IntArray(2)
        val burstLocation = IntArray(2)
        view.getLocationInWindow(viewLocation)
        sparkleBurst.getLocationInWindow(burstLocation)
        val centerX = if (view.width > 0) view.width / 2f else dp(12f).toFloat()
        val centerY = if (view.height > 0) view.height / 2f else dp(12f).toFloat()
        sparkleBurst.burst(
            viewLocation[0] - burstLocation[0] + centerX,
            viewLocation[1] - burstLocation[1] + centerY,
            color
        )
    }

    private fun accentColor(): Int {
        return when (profile?.gender) {
            FriendGender.FEMALE -> 0xFFFF7AB6.toInt()
            FriendGender.MALE -> 0xFF5CE1E6.toInt()
            null -> 0xFFFF7AB6.toInt()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        currentFocus?.clearFocus()
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun nextId(): Long {
        idCounter += 1
        return System.currentTimeMillis() * 1000L + idCounter
    }

    override fun onPause() {
        super.onPause()
        saveChat()
    }

    override fun onDestroy() {
        chatGeneration += 1
        chatVisible = false
        waitingForReply = false
        handler.removeCallbacksAndMessages(null)
        onlinePulse?.cancel()
        if (::gigaChat.isInitialized) {
            gigaChat.close()
        }
        super.onDestroy()
    }
}
