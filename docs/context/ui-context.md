# UI Context

## Theme

The NakPom mobile application uses a clean, modern design language optimized for Cambodian users. The design emphasizes readability, cultural appropriateness, and ease of use. The app supports both light and dark modes with a primary focus on accessibility and clarity.

## Colors

Define your color tokens as CSS custom properties or Android color resources. All components must use these tokens — no hardcoded hex values.

### Android Color Resources (colors.xml)

| Role            | Resource Name      | Value    |
| --------------- | ------------------ | -------- |
| Primary         | `colorPrimary`     | `#4CAF50` |
| Primary Dark    | `colorPrimaryDark`  | `#388E3C` |
| Accent          | `colorAccent`      | `#FF9800` |
| Background      | `colorBackground`  | `#FFFFFF` |
| Surface         | `colorSurface`     | `#F5F5F5` |
| Error           | `colorError`       | `#F44336` |
| On Primary      | `colorOnPrimary`   | `#FFFFFF` |
| On Background   | `colorOnBackground`| `#212121` |
| On Surface      | `colorOnSurface`   | `#424242` |
| Success         | `colorSuccess`     | `#4CAF50` |

## Typography

| Role      | Font Family        | Weight  |
| --------- | ----------------- | ------- |
| Body      | Roboto            | Regular |
| Heading   | Roboto            | Medium  |
| Khmer     | Khmer OS Battambang| Regular |
| Button    | Roboto            | Medium  |
| Caption   | Roboto            | Regular |

## Border Radius

| Context           | Value (dp) |
| ----------------- | ---------- |
| Buttons           | 8dp        |
| Cards             | 12dp       |
| Dialogs           | 16dp       |
| Input fields      | 8dp        |
| Chips/Badges      | 16dp       |

## Component Library

Jetpack Compose with Material3 components. Use Material3 composables as the base and customize with NakPom theme. Components live in `ui/components/` package. Prefer built-in Material3 components over custom implementations.

## Layout Patterns

- **Registration/Login**: Centered card layout with logo at top, form fields below, action button at bottom
- **Family Space**: List view with family name, invite code, and member count
- **Navigation**: Bottom navigation bar with 3-4 main sections
- **Forms**: Vertical stack with labels above input fields, helper text below
- **Modals**: Centered overlay with backdrop blur

## Icons

Material Icons (Google). Use vector drawables for icons. Common icon sizes:
- Inline: 16dp
- Buttons: 24dp
- Navigation: 24dp
- Large: 48dp

## Khmer Language Support

- Use Khmer OS Battambang font for Khmer text
- Ensure proper text rendering for Khmer characters
- Test UI with Khmer text to verify layout doesn't break
- Support both Khmer and English languages
- Language toggle in settings

## Spacing

Use 8dp grid system for consistent spacing:
- Extra small: 4dp
- Small: 8dp
- Medium: 16dp
- Large: 24dp
- Extra large: 32dp

## Screen Sizes

Support the following screen sizes:
- Small phones: 360dp width
- Standard phones: 360-384dp width
- Large phones: 384-412dp width
- Tablets: 600dp+ width (future consideration)

## Accessibility

- Minimum touch target size: 48dp
- Color contrast ratio: 4.5:1 for normal text, 3:1 for large text
- Support TalkBack for screen readers
- Provide content descriptions for icons
- Use semantic labels for form fields
