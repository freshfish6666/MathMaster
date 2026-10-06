$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$output = Join-Path $projectRoot 'build\pollution-whisper-sources'
New-Item -ItemType Directory -Force -Path $output | Out-Null

$lines = @(
    @{ File = 'latin_1.wav'; Voice = 'Microsoft Zira Desktop'; Rate = -3; Text = 'Velora beneath le nombre sans visage, unter dem leeren null, kage no su, bin su ga doram.' },
    @{ File = 'latin_2.wav'; Voice = 'Microsoft Zira Desktop'; Rate = -4; Text = 'Do not count. Ne compte pas. Zahl ohne Mund. Rei wa mada. Yeong eui mun. Ling zai kan ni.' },
    @{ File = 'latin_3.wav'; Voice = 'Microsoft Zira Desktop'; Rate = -2; Text = 'Par la racine, durch das Fleisch, kuro na equation, gong setsu, jeo pyeon zero.' },
    @{ File = 'latin_4.wav'; Voice = 'Microsoft Zira Desktop'; Rate = -5; Text = 'Nul avedra, kein ende, mu no koe, hana dul null, ling men shuo, varo senkai.' },
    @{ File = 'latin_5.wav'; Voice = 'Microsoft Zira Desktop'; Rate = -3; Text = 'The cardinal whispers, la somme respire, die Zahl erinnert, yami no limen, gong heo seora.' },
    @{ File = 'latin_6.wav'; Voice = 'Microsoft Zira Desktop'; Rate = -4; Text = 'Aucune bouche, keine Augen, dare mo inai, amugeotdo eopseo, ling ling velorum.' },
    @{ File = 'han_1.wav'; Voice = 'Microsoft Kangkang'; Rate = -4; Text = '零在门后数着你的影子，诺姆布赫桑维萨日，安特努尔，目无扣诶，宾苏卡多拉。' },
    @{ File = 'han_2.wav'; Voice = 'Microsoft Huihui Desktop'; Rate = -3; Text = '不要完成这个证明，内空特巴，扎欧内蒙德，累哇马达，永也梦。' },
    @{ File = 'han_3.wav'; Voice = 'Microsoft Kangkang'; Rate = -5; Text = '根在血肉里呼吸，帕拉哈辛，杜赫达斯弗莱施，库罗那伊奎真，空色。' },
    @{ File = 'han_4.wav'; Voice = 'Microsoft Huihui Desktop'; Rate = -2; Text = '没有意义的意义正在返回，努拉维德拉，凯恩恩德，哈那杜努尔，零门说。' },
    @{ File = 'han_5.wav'; Voice = 'Microsoft Kangkang'; Rate = -4; Text = '数不是数，名不是名，卡尔迪纳尔，拉松么，迪扎尔，亚米诺里门。' },
    @{ File = 'han_6.wav'; Voice = 'Microsoft Huihui Desktop'; Rate = -5; Text = '空集睁开眼睛，奥坤布什，凯内奥根，达累莫伊那伊，阿木郭多欧布索。' }
)

foreach ($line in $lines) {
    $synth = [System.Speech.Synthesis.SpeechSynthesizer]::new()
    try {
        $synth.SelectVoice($line.Voice)
        $synth.Rate = $line.Rate
        $synth.Volume = 72
        $path = Join-Path $output $line.File
        $synth.SetOutputToWaveFile($path)
        $synth.Speak($line.Text)
    }
    finally {
        $synth.Dispose()
    }
}

Write-Output "Generated $($lines.Count) multilingual voice sources in $output"
