// color-theme-switcher.js
document.addEventListener('DOMContentLoaded', function () {
  console.log('Theme switcher initialized');
  // Define your color themes here
  const themes = {
    autumn: {
      primary: '#773344',
      primaryLight: '#91455a',
      primaryDark: '#5d272f',
      secondary: '#E3B5A4',
      secondaryLight: '#edd0c5',
      secondaryDark: '#d99a83',
      accent: '#F4AC45',
      accentLight: '#f6bd6c',
      accentDark: '#f29b1e',
      neutral: '#EFE2BA',
      neutralLight: '#f9f4e3',
      neutralDark: '#e5d091',
    },
    factory: {
      primary: '#333652',
      primaryLight: '#4a4d6a',
      primaryDark: '#252840',
      secondary: '#90adc6',
      secondaryLight: '#a5bfd4',
      secondaryDark: '#7a96ad',
      accent: '#fad02c',
      accentLight: '#fbda58',
      accentDark: '#e9bf1a',
      neutral: '#e9eaec',
      neutralLight: '#f5f6f7',
      neutralDark: '#d0d2d6',
    },
    // Add as many themes as you want
    forest: {
      primary: '#2D3E40',
      primaryLight: '#435759',
      primaryDark: '#1d2829',
      secondary: '#7A9E7E',
      secondaryLight: '#97b79a',
      secondaryDark: '#638266',
      accent: '#E7BB41',
      accentLight: '#ecc869',
      accentDark: '#e1aa25',
      neutral: '#DBEFBC',
      neutralLight: '#e9f5d4',
      neutralDark: '#cee9a4',
    },
    // Additional 20 color themes
    sunset: {
      primary: '#1A1A2E',
      primaryLight: '#31314e',
      primaryDark: '#0d0d17',
      secondary: '#E94560',
      secondaryLight: '#ee6a80',
      secondaryDark: '#e12142',
      accent: '#F9ED69',
      accentLight: '#faf389',
      accentDark: '#f8e749',
      neutral: '#F1F1F1',
      neutralLight: '#ffffff',
      neutralDark: '#d8d8d8',
    },
    lavender: {
      primary: '#48466D',
      primaryLight: '#5f5c8a',
      primaryDark: '#343256',
      secondary: '#3D84A8',
      secondaryLight: '#539abe',
      secondaryDark: '#2d6a8a',
      accent: '#ABEDD8',
      accentLight: '#c3f2e3',
      accentDark: '#93e8cd',
      neutral: '#E9E9E9',
      neutralLight: '#ffffff',
      neutralDark: '#d0d0d0',
    },
    vintage: {
      primary: '#845EC2',
      primaryLight: '#9a7bce',
      primaryDark: '#6c3fba',
      secondary: '#FF5E78',
      secondaryLight: '#ff8296',
      secondaryDark: '#ff3a5a',
      accent: '#FFC75F',
      accentLight: '#ffd483',
      accentDark: '#ffba3b',
      neutral: '#F9F9F9',
      neutralLight: '#ffffff',
      neutralDark: '#e0e0e0',
    },
    pastel: {
      primary: '#364F6B',
      primaryLight: '#466788',
      primaryDark: '#263a4f',
      secondary: '#FC5185',
      secondaryLight: '#fd779f',
      secondaryDark: '#fb2b6b',
      accent: '#F5F5F5',
      accentLight: '#ffffff',
      accentDark: '#dcdcdc',
      neutral: '#3FC1C9',
      neutralLight: '#63cdd3',
      neutralDark: '#2fb0b8',
    },
    berry: {
      primary: '#7C3C66',
      primaryLight: '#9A4C80',
      primaryDark: '#5D2C4C',
      secondary: '#E65891',
      secondaryLight: '#EB7DA7',
      secondaryDark: '#E1337B',
      accent: '#FFDEE2',
      accentLight: '#FFF2F5',
      accentDark: '#FFCACF',
      neutral: '#F0D9DA',
      neutralLight: '#F8E9EA',
      neutralDark: '#E8C9CA',
    },
    coffee: {
      primary: '#4B3832',
      primaryLight: '#6A514A',
      primaryDark: '#2C211A',
      secondary: '#854442',
      secondaryLight: '#A35553',
      secondaryDark: '#673231',
      accent: '#BE9B7B',
      accentLight: '#CCB199',
      accentDark: '#B0855D',
      neutral: '#FFF4E6',
      neutralLight: '#FFFBF7',
      neutralDark: '#FFEDD5',
    },
    tech: {
      primary: '#0D1321',
      primaryLight: '#1A2540',
      primaryDark: '#000000',
      secondary: '#3E5C76',
      secondaryLight: '#4F7195',
      secondaryDark: '#2D4757',
      accent: '#48ACF0',
      accentLight: '#6FBDF3',
      accentDark: '#219BED',
      neutral: '#F0EBD8',
      neutralLight: '#F7F4EA',
      neutralDark: '#E9E2C6',
    },
    nordic: {
      primary: '#2E3440',
      primaryLight: '#434C5E',
      primaryDark: '#191C22',
      secondary: '#81A1C1',
      secondaryLight: '#9FBAD7',
      secondaryDark: '#6388AB',
      accent: '#EBCB8B',
      accentLight: '#F0D9A5',
      accentDark: '#E6BD71',
      neutral: '#E5E9F0',
      neutralLight: '#ECEFF4',
      neutralDark: '#D8DEE9',
    },
    retro: {
      primary: '#2B2D42',
      primaryLight: '#3D4061',
      primaryDark: '#191A23',
      secondary: '#D80032',
      secondaryLight: '#F60039',
      secondaryDark: '#B5002B',
      accent: '#F8F32B',
      accentLight: '#F9F555',
      accentDark: '#F7F101',
      neutral: '#EDF2F4',
      neutralLight: '#FFFFFF',
      neutralDark: '#CDD6DD',
    },
    marine: {
      primary: '#05386B',
      primaryLight: '#084F97',
      primaryDark: '#02213F',
      secondary: '#5CDB95',
      secondaryLight: '#7DE5AA',
      secondaryDark: '#3BD180',
      accent: '#EDF5E1',
      accentLight: '#F7FAEA',
      accentDark: '#E3F0D8',
      neutral: '#8EE4AF',
      neutralLight: '#A9EBC3',
      neutralDark: '#73DD9B',
    },
    candy: {
      primary: '#6C48B8',
      primaryLight: '#8566C9',
      primaryDark: '#533AA7',
      secondary: '#FF70A6',
      secondaryLight: '#FF93BD',
      secondaryDark: '#FF4D8F',
      accent: '#FFF066',
      accentLight: '#FFF485',
      accentDark: '#FFEC47',
      neutral: '#E8F7EE',
      neutralLight: '#F6FDF9',
      neutralDark: '#DAF1E3',
    },
    earth: {
      primary: '#4A5859',
      primaryLight: '#616F70',
      primaryDark: '#334142',
      secondary: '#A18276',
      secondaryLight: '#B49C92',
      secondaryDark: '#8E685A',
      accent: '#D8B894',
      accentLight: '#E3C9AD',
      accentDark: '#CDA77B',
      neutral: '#EFE7D3',
      neutralLight: '#F8F3E9',
      neutralDark: '#E6DBBD',
    },
    monochrome: {
      primary: '#222222',
      primaryLight: '#404040',
      primaryDark: '#040404',
      secondary: '#666666',
      secondaryLight: '#858585',
      secondaryDark: '#474747',
      accent: '#DDDDDD',
      accentLight: '#EEEEEE',
      accentDark: '#CCCCCC',
      neutral: '#F5F5F5',
      neutralLight: '#FFFFFF',
      neutralDark: '#E8E8E8',
    },
    desert: {
      primary: '#A24936',
      primaryLight: '#C05A43',
      primaryDark: '#843829',
      secondary: '#D36135',
      secondaryLight: '#DB7D59',
      secondaryDark: '#BA4F26',
      accent: '#EFC88B',
      accentLight: '#F4D7A8',
      accentDark: '#EAB96E',
      neutral: '#EBEBD3',
      neutralLight: '#F5F5E8',
      neutralDark: '#E1E1BE',
    },
    mint: {
      primary: '#375954',
      primaryLight: '#4A756E',
      primaryDark: '#243D3A',
      secondary: '#ACBFA4',
      secondaryLight: '#BDCCB8',
      secondaryDark: '#9BB290',
      accent: '#E8DDB5',
      accentLight: '#EFE8CA',
      accentDark: '#E1D2A0',
      neutral: '#EDC79B',
      neutralLight: '#F2D7B4',
      neutralDark: '#E8B782',
    },
    royal: {
      primary: '#10316B',
      primaryLight: '#164593',
      primaryDark: '#0A1D43',
      secondary: '#0B409C',
      secondaryLight: '#0E51C3',
      secondaryDark: '#082F75',
      accent: '#FDBE34',
      accentLight: '#FDCC5D',
      accentDark: '#FDB00B',
      neutral: '#D9EDBF',
      neutralLight: '#E7F4D4',
      neutralDark: '#CBE6AA',
    },
  };

  // Function to apply a theme
  function applyTheme(themeName) {
    const theme = themes[themeName] || themes.autumn;

    console.log('Applying theme:', themeName, theme);

    // Update CSS variables
    document.documentElement.style.setProperty(
      '--color-primary',
      theme.primary,
    );
    document.documentElement.style.setProperty(
      '--color-primary-light',
      theme.primaryLight,
    );
    document.documentElement.style.setProperty(
      '--color-primary-dark',
      theme.primaryDark,
    );
    document.documentElement.style.setProperty(
      '--color-secondary',
      theme.secondary,
    );
    document.documentElement.style.setProperty(
      '--color-secondary-light',
      theme.secondaryLight,
    );
    document.documentElement.style.setProperty(
      '--color-secondary-dark',
      theme.secondaryDark,
    );
    document.documentElement.style.setProperty('--color-accent', theme.accent);
    document.documentElement.style.setProperty(
      '--color-accent-light',
      theme.accentLight,
    );
    document.documentElement.style.setProperty(
      '--color-accent-dark',
      theme.accentDark,
    );
    document.documentElement.style.setProperty(
      '--color-neutral',
      theme.neutral,
    );
    document.documentElement.style.setProperty(
      '--color-neutral-light',
      theme.neutralLight,
    );
    document.documentElement.style.setProperty(
      '--color-neutral-dark',
      theme.neutralDark,
    );

    // Update Tailwind classes by forcing a refresh
    updateTailwindClasses(theme);

    // Save the current theme preference
    localStorage.setItem('preferredTheme', themeName);
  }

  // Function to update Tailwind classes
  function updateTailwindClasses(theme) {
    // Create or update the style element for dynamic Tailwind classes
    let styleElement = document.getElementById('dynamic-theme-styles');
    if (!styleElement) {
      styleElement = document.createElement('style');
      styleElement.id = 'dynamic-theme-styles';
      document.head.appendChild(styleElement);
    }

    // Create CSS that will override Tailwind classes with our theme colors
    styleElement.textContent = `
      .bg-primary { background-color: ${theme.primary} !important; }
      .bg-primary-light { background-color: ${theme.primaryLight} !important; }
      .bg-primary-dark { background-color: ${theme.primaryDark} !important; }
      .bg-secondary { background-color: ${theme.secondary} !important; }
      .bg-secondary-light { background-color: ${theme.secondaryLight} !important; }
      .bg-secondary-dark { background-color: ${theme.secondaryDark} !important; }
      .bg-accent { background-color: ${theme.accent} !important; }
      .bg-accent-light { background-color: ${theme.accentLight} !important; }
      .bg-accent-dark { background-color: ${theme.accentDark} !important; }
      .bg-neutral { background-color: ${theme.neutral} !important; }
      .bg-neutral-light { background-color: ${theme.neutralLight} !important; }
      .bg-neutral-dark { background-color: ${theme.neutralDark} !important; }

      .text-primary { color: ${theme.primary} !important; }
      .text-primary-light { color: ${theme.primaryLight} !important; }
      .text-primary-dark { color: ${theme.primaryDark} !important; }
      .text-secondary { color: ${theme.secondary} !important; }
      .text-secondary-light { color: ${theme.secondaryLight} !important; }
      .text-secondary-dark { color: ${theme.secondaryDark} !important; }
      .text-accent { color: ${theme.accent} !important; }
      .text-accent-light { color: ${theme.accentLight} !important; }
      .text-accent-dark { color: ${theme.accentDark} !important; }
      .text-neutral { color: ${theme.neutral} !important; }
      .text-neutral-light { color: ${theme.neutralLight} !important; }
      .text-neutral-dark { color: ${theme.neutralDark} !important; }

      .border-primary { border-color: ${theme.primary} !important; }
      .border-secondary { border-color: ${theme.secondary} !important; }
      .border-accent { border-color: ${theme.accent} !important; }
      .border-neutral { border-color: ${theme.neutral} !important; }
    `;
  }

  // Create a theme switcher UI
  function createThemeSwitcher() {
    console.log('Creating theme switcher UI');

    const container = document.getElementById('theme-switcher');
    container.className = 'fixed top-5 right-5 z-50';

    const button = document.getElementById('themeToggle');
    //button.className = 'bg-primary text-white px-3 py-2 rounded-lg shadow-md hover:bg-primary-dark transition-colors';
    //button.textContent = 'Theme';
    //button.style.backgroundColor = themes.default.primary;
    //button.style.color = '#ffffff';

    const dropdown = document.createElement('div');
    dropdown.className =
      'absolute right-0 mt-2 w-64 bg-white rounded-md shadow-lg hidden overflow-y-auto max-h-96';
    dropdown.style.backgroundColor = '#ffffff';

    // Create search input
    const searchContainer = document.createElement('div');
    searchContainer.className = 'p-3 border-b';

    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.placeholder = 'Search themes...';
    searchInput.className = 'w-full px-3 py-2 border rounded-md text-sm';
    searchInput.style.borderColor = '#e2e8f0';

    searchContainer.appendChild(searchInput);
    dropdown.appendChild(searchContainer);

    // Container for theme options
    const optionsContainer = document.createElement('div');
    optionsContainer.className = 'py-2';
    dropdown.appendChild(optionsContainer);

    // Add theme options
    function renderThemeOptions(filter = '') {
      // Clear existing options
      optionsContainer.innerHTML = '';

      // Filter themes
      const filteredThemes = Object.keys(themes).filter((name) =>
        name.toLowerCase().includes(filter.toLowerCase()),
      );

      // Display message if no results
      if (filteredThemes.length === 0) {
        const noResults = document.createElement('div');
        noResults.className = 'px-4 py-2 text-sm text-gray-500 italic';
        noResults.textContent = 'No themes found';
        optionsContainer.appendChild(noResults);
        return;
      }

      filteredThemes.forEach((themeName) => {
        const option = document.createElement('button');
        option.className =
          'block w-full text-left px-4 py-3 text-sm transition-colors hover:bg-gray-100';
        option.style.color = '#333333';

        const theme = themes[themeName];

        // Create theme option with preview
        const content = document.createElement('div');

        // Theme name
        const nameEl = document.createElement('div');
        nameEl.className = 'font-medium mb-1';
        nameEl.textContent =
          themeName.charAt(0).toUpperCase() + themeName.slice(1);
        content.appendChild(nameEl);

        // Color preview container
        const previewContainer = document.createElement('div');
        previewContainer.className = 'flex gap-1';

        // Add color swatches
        const colors = [
          { color: theme.primary, label: 'P' },
          { color: theme.secondary, label: 'S' },
          { color: theme.accent, label: 'A' },
          { color: theme.neutral, label: 'N' },
        ];

        colors.forEach(({ color, label }) => {
          const swatch = document.createElement('div');
          swatch.className =
            'w-6 h-6 rounded-full flex items-center justify-center text-xs';
          swatch.style.backgroundColor = color;
          swatch.style.color = isLightColor(color) ? '#333' : '#fff';
          //swatch.textContent = label;
          previewContainer.appendChild(swatch);
        });

        content.appendChild(previewContainer);
        option.appendChild(content);

        option.onclick = () => {
          console.log('Theme option clicked:', themeName);
          applyTheme(themeName);
          dropdown.classList.add('hidden');
        };

        optionsContainer.appendChild(option);
      });
    }

    // Helper to determine if a color is light (for text contrast)
    function isLightColor(color) {
      // Convert hex to RGB
      const hex = color.replace('#', '');
      const r = parseInt(hex.substr(0, 2), 16);
      const g = parseInt(hex.substr(2, 2), 16);
      const b = parseInt(hex.substr(4, 2), 16);

      // Calculate brightness (YIQ equation)
      const brightness = (r * 299 + g * 587 + b * 114) / 1000;
      return brightness > 128;
    }

    // Initial render
    renderThemeOptions();

    // Handle search input
    searchInput.addEventListener('input', (e) => {
      renderThemeOptions(e.target.value);
    });

    // Toggle dropdown
    button.onclick = () => {
      dropdown.classList.toggle('hidden');
      if (!dropdown.classList.contains('hidden')) {
        searchInput.focus();
        searchInput.value = '';
        renderThemeOptions();
      }
    };

    // Close dropdown when clicking outside
    document.addEventListener('click', (event) => {
      if (!container.contains(event.target)) {
        dropdown.classList.add('hidden');
      }
    });

    // Toggle dropdown
    button.onclick = () => {
      dropdown.classList.toggle('hidden');
    };

    // Close dropdown when clicking outside
    document.addEventListener('click', (event) => {
      if (!container.contains(event.target)) {
        dropdown.classList.add('hidden');
      }
    });

    container.appendChild(button);
    container.appendChild(dropdown);
    document.body.appendChild(container);
  }

  // Apply the saved theme or default
  const savedTheme = localStorage.getItem('preferredTheme');
  applyTheme(savedTheme || 'autumn');

  // Create the theme switcher UI
  createThemeSwitcher();
});
