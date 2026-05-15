<?php
require_once __DIR__ . '/includes/layout.php';
$docCode = (string)($_GET['doc'] ?? '');
$doc = null;
if ($docCode !== '') {
    $stmt = db()->prepare('SELECT * FROM public_documents WHERE doc_code = ?');
    $stmt->execute([$docCode]);
    $doc = $stmt->fetch();
}
$docs = db()->query('SELECT doc_code, title, file_name FROM public_documents ORDER BY doc_code')->fetchAll();
page_header('Documents');
?>
<section class="panel">
  <p class="eyebrow">Documents</p>
  <h1>Public financing documents</h1>
  <p class="muted">Document IDs are assigned by finance operations and mirrored into public storage when approved.</p>
  <div class="pill-row">
    <?php foreach ($docs as $item): ?>
      <a class="pill" href="/documents.php?doc=<?= e($item['doc_code']) ?>"><?= e($item['doc_code']) ?></a>
    <?php endforeach; ?>
  </div>
</section>
<?php if ($doc): ?>
<section class="panel">
  <h2><?= e($doc['title']) ?></h2>
  <pre class="code"><?= e($doc['body']) ?></pre>
  <?php if ($doc['file_name']): ?>
    <a class="button secondary" href="/download.php?file=public_docs/<?= e($doc['file_name']) ?>">Download mirror</a>
  <?php endif; ?>
</section>
<?php endif; ?>
<?php page_footer(); ?>
