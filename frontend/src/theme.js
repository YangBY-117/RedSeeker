// 主题配置 - 邮觅红途红色主题
export const theme = {
    // 品牌色系
    colors: {
      primary: '#c62828',      // 主红色 - 深红色
      primaryLight: '#ff5f52', // 浅红色
      primaryDark: '#8e0000',  // 深红色
      secondary: '#212121',    // 深灰色
      secondaryLight: '#484848',
      accent: '#ff9800',       // 强调色 - 橙色
      background: '#fafafa',
      surface: '#ffffff',
      text: {
        primary: '#212121',
        secondary: '#757575',
        disabled: '#9e9e9e',
        hint: '#bdbdbd'
      },
      error: '#d32f2f',
      warning: '#ffa000',
      success: '#388e3c',
      info: '#1976d2'
    },
    
    // 字体
    typography: {
      fontFamily: '"PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif',
      fontSize: {
        xs: '12px',
        sm: '14px',
        base: '16px',
        lg: '18px',
        xl: '20px',
        '2xl': '24px',
        '3xl': '30px',
        '4xl': '36px'
      },
      fontWeight: {
        light: 300,
        normal: 400,
        medium: 500,
        bold: 700
      }
    },
    
    // 间距
    spacing: {
      0: '0',
      1: '4px',
      2: '8px',
      3: '12px',
      4: '16px',
      5: '20px',
      6: '24px',
      8: '32px',
      10: '40px',
      12: '48px',
      16: '64px'
    },
    
    // 圆角
    borderRadius: {
      sm: '4px',
      md: '8px',
      lg: '12px',
      xl: '16px',
      '2xl': '24px',
      full: '9999px'
    },
    
    // 阴影
    shadows: {
      sm: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)',
      md: '0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23)',
      lg: '0 10px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23)',
      xl: '0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)'
    },
    
    // 动画
    transitions: {
      fast: '150ms ease',
      normal: '300ms ease',
      slow: '500ms ease'
    }
  }